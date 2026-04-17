package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.ActivationRequest;
import friasoft.gn.schoolapp.dto.InviteUserDTO;
import friasoft.gn.schoolapp.entity.auth.Activation;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.repository.IActivationRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@AllArgsConstructor
@Service
public class UserService implements UserDetailsService{

    private UserRepository userRepository;
    private IActivationRepository iActivationRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private NotificationService notificationService;

    public void sendActivationEmail(User user) {
        Activation activation = createActivation(user);
        this.notificationService.sendActivationMail(activation);
    }

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE', 'SUPER_ADMIN')")
    public List<User> getAll() {
        User current = getUserInfo();
        if (current == null) {
            return List.of();
        }
        return this.userRepository.findAll().stream()
            // Ne jamais exposer les comptes super admin dans la gestion tenant.
            .filter(u -> u.getRole() != User.UserRole.SUPER_ADMIN)
            .filter(u -> {
                if (current.getRole() == User.UserRole.SUPER_ADMIN) {
                    return true;
                }
                Long tenantId = current.getTenantId();
                return tenantId != null && tenantId.equals(u.getTenantId());
            })
            .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN_ECOLE')")
    public String inviteUser(InviteUserDTO request) {
        User current = getUserInfo();
        if (current == null || current.getTenantId() == null) {
            throw new RuntimeException("Contexte établissement introuvable");
        }
        if (request.nom() == null || request.nom().isBlank()) {
            throw new RuntimeException("Nom prénom obligatoire");
        }
        if (request.email() == null || !request.email().contains("@")) {
            throw new RuntimeException("Email invalide");
        }
        User.UserRole role = request.role();
        if (role == null) {
            throw new RuntimeException("Rôle obligatoire");
        }
        if (role != User.UserRole.ADMIN_ECOLE && role != User.UserRole.STAFF && role != User.UserRole.TEACHER) {
            throw new RuntimeException("Rôle non autorisé");
        }
        if (this.userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email deja utilisé");
        }

        User user = new User();
        user.setUsername(request.email());
        user.setFullname(request.nom().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setRole(role);
        user.setTenantId(current.getTenantId());
        user.setSchool(null);
        user.setPassword(this.passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setActive(false);
        user = this.userRepository.save(user);

        Activation activation = createActivation(user);
        String activationCode = activation.getCode();
        log.info("Activation code for invited user {}: {}", user.getEmail(), activationCode);
        return activationCode;
    }

    public void activate(ActivationRequest activation) {
        if (activation.email() == null || activation.email().isBlank()) {
            throw new RuntimeException("Email obligatoire");
        }
        if (activation.activationCode() == null || activation.activationCode().isBlank()) {
            throw new RuntimeException("Code d'activation obligatoire");
        }
        if (activation.newPassword() == null || activation.newPassword().isBlank()) {
            throw new RuntimeException("Nouveau mot de passe obligatoire");
        }

        Activation savedActivation = this.iActivationRepository.findByCode(activation.activationCode())
            .orElseThrow(() -> new RuntimeException("Activation code invalide"));
        User user = this.userRepository.findByEmail(activation.email())
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        if(Instant.now().isBefore(savedActivation.getExpiration()) && savedActivation.getUser().getEmail().equals(user.getEmail())) {
            user.setPassword(this.passwordEncoder.encode(activation.newPassword()));
            user.setActive(true);
            this.userRepository.save(user);
            this.iActivationRepository.delete(savedActivation);
        } else {
            throw new RuntimeException("Activation expired");
        }
    }

    public void resetPassword(Map<String, String> request) {
        User user = this.loadUserByUsername(request.get("email"));
        Activation activation = createActivation(user);
        this.notificationService.sendResetPassWordMail(activation);
    }

    public void updatePassword(Map<String, String> request) {
        Activation savedActivation = this.iActivationRepository.findByCode(request.get("code"))
        .orElseThrow(() -> new RuntimeException("Activation code invalide"));
        User user = this.userRepository.findByEmail(request.get("email")).orElseThrow();
        if(Instant.now().isBefore(savedActivation.getExpiration()) && savedActivation.getUser().getEmail().equals(user.getEmail())) {
            user.setPassword(this.passwordEncoder.encode(request.get("password")));
            this.userRepository.save(user);
            this.iActivationRepository.delete(savedActivation);
        } else {
            throw new RuntimeException("Activation expired");
        }
    }

    public void saveUser(User user) {
        this.userRepository.save(user);
    }

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.userRepository
            .findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("Pas d'utilisateur pour cet identifiant"));
    }

    public User getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    private Activation createActivation(User user) {
        Random random = new Random();
        Activation activation = new Activation();
        activation.setCode(String.format("%06d", random.nextInt(999999)));
        activation.setRegistrationDate(Instant.now());
        activation.setExpiration(Instant.now().plusMillis(30 * 60 * 1000));
        activation.setUser(user);
        return iActivationRepository.save(activation);
    }

    public List<User> getAdminBySchool(Long schoolId) {
        List<User> actualList = new ArrayList<>();
        this.userRepository.findAllBySchoolId(schoolId).iterator().forEachRemaining(actualList::add);
        return actualList;
    }

    public List<User> getUnassignedUsers(String term) {
        return this.userRepository.searchByKeywordAndNoSchool(term);
    }

}
