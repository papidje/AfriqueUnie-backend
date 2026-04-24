package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.ActivationRequest;
import friasoft.gn.schoolapp.dto.InviteUserDTO;
import friasoft.gn.schoolapp.entity.auth.Activation;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.repository.IActivationRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final SchoolService schoolService;

    /** Crée une activation, envoie le mail et retourne l’enregistrement (ex. code pour l’API d’invitation). */
    public Activation sendActivationEmail(User user) {
        Activation activation = createActivation(user);
        this.notificationService.sendActivationMail(activation);
        return activation;
    }

    /**
     * Remplace toute activation existante pour l’utilisateur, en enregistre une nouvelle et envoie le mail.
     * Réservé aux comptes encore inactifs (invitation non finalisée).
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN_ECOLE', 'DIRECTOR')")
    public void resendActivationEmail(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant utilisateur obligatoire.");
        }
        User current = getUserInfo();
        if (current == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        User target = this.userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        assertCurrentUserMayManageTargetUser(current, target);
        if (target.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce compte est déjà activé.");
        }
        if (target.getEmail() == null || target.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune adresse e-mail pour cet utilisateur.");
        }
        sendActivationEmail(target);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN_ECOLE', 'DIRECTOR')")
    public List<User> getAll() {
        User current = getUserInfo();
        if (current == null) {
            return List.of();
        }
        if (current.getRole() == User.UserRole.DIRECTOR) {
            School dirSchool = current.getSchool();
            if (dirSchool == null || dirSchool.getId() == null) {
                return List.of();
            }
            Long dirSchoolId = dirSchool.getId();
            return this.userRepository.findAll().stream()
                .filter(u -> u.getRole() != User.UserRole.SUPER_ADMIN)
                .filter(u -> u.getRole() == User.UserRole.TEACHER || u.getRole() == User.UserRole.ACCOUNTANT)
                .filter(u -> u.getSchool() != null && dirSchoolId.equals(u.getSchool().getId()))
                .toList();
        }
        return this.userRepository.findAll().stream()
            .filter(u -> u.getRole() != User.UserRole.SUPER_ADMIN)
            .filter(u -> {
                Long tenantId = current.getOrganizationTenantId() != null
                    ? current.getOrganizationTenantId()
                    : current.getTenantId();
                Long uTenant = u.getOrganizationTenantId() != null ? u.getOrganizationTenantId() : u.getTenantId();
                return tenantId != null && tenantId.equals(uTenant);
            })
            .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN_ECOLE', 'DIRECTOR')")
    public String inviteUser(InviteUserDTO request) {
        User current = getUserInfo();
        if (current == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        User.UserRole currentRole = current.getRole();
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
        if (this.userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email deja utilisé");
        }

        User user = new User();
        user.setUsername(request.email());
        user.setFullname(request.nom().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setRole(role);

        if (currentRole == User.UserRole.DIRECTOR) {
            if (role != User.UserRole.TEACHER && role != User.UserRole.ACCOUNTANT) {
                throw new RuntimeException("Rôle non autorisé pour un directeur");
            }
            School dirSchool = current.getSchool();
            if (dirSchool == null || dirSchool.getId() == null) {
                throw new RuntimeException("École du directeur introuvable");
            }
            schoolService.assertCurrentUserCanAccessSchool(dirSchool.getId());
            user.setSchool(dirSchool);
            user.setTenantId(dirSchool.getTenantId());
        } else if (currentRole == User.UserRole.ADMIN_ECOLE) {
            Long orgTenant = current.getOrganizationTenantId();
            if (orgTenant == null && current.getTenantId() == null) {
                throw new RuntimeException("Contexte établissement introuvable");
            }
            if (role != User.UserRole.ADMIN_ECOLE
                && role != User.UserRole.STAFF
                && role != User.UserRole.TEACHER
                && role != User.UserRole.DIRECTOR) {
                throw new RuntimeException("Rôle non autorisé");
            }
            if (role == User.UserRole.ADMIN_ECOLE) {
                if (request.schoolId() != null) {
                    throw new RuntimeException("schoolId ne doit pas être fourni pour un ADMIN_ECOLE");
                }
                user.setSchool(null);
                user.setTenantId(orgTenant != null ? orgTenant : current.getTenantId());
            } else if (role == User.UserRole.DIRECTOR) {
                if (request.schoolId() == null) {
                    throw new RuntimeException("schoolId obligatoire pour un directeur");
                }
                schoolService.assertCurrentUserCanAccessSchool(request.schoolId());
                School linked = schoolService.getSchool(request.schoolId());
                user.setSchool(linked);
                user.setTenantId(linked.getTenantId());
            } else {
                if (request.schoolId() == null) {
                    throw new RuntimeException("schoolId obligatoire pour ce rôle");
                }
                schoolService.assertCurrentUserCanAccessSchool(request.schoolId());
                School linked = schoolService.getSchool(request.schoolId());
                user.setSchool(linked);
                user.setTenantId(linked.getTenantId());
            }
        } else {
            throw new RuntimeException("Action non autorisée");
        }

        user.setPassword(this.passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setActive(false);
        user = this.userRepository.save(user);

        Activation activation = sendActivationEmail(user);
        String activationCode = activation.getCode();
        log.info("Invitation : mail d'activation envoyé à {} (code trace serveur : {})", user.getEmail(), activationCode);
        return activationCode;
    }

    @Transactional
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
            this.notificationService.sendAccountActivatedMail(user);
        } else {
            throw new RuntimeException("Activation expired");
        }
    }

    @Transactional
    public void resetPassword(Map<String, String> request) {
        User user = this.loadUserByUsername(request.get("email"));
        Activation activation = createActivation(user);
        this.notificationService.sendResetPassWordMail(activation);
    }

    @Transactional
    public void updatePassword(Map<String, String> request) {
        Activation savedActivation = this.iActivationRepository.findByCode(request.get("code"))
        .orElseThrow(() -> new RuntimeException("Activation code invalide"));
        User user = this.userRepository.findByEmail(request.get("email")).orElseThrow();
        if(Instant.now().isBefore(savedActivation.getExpiration()) && savedActivation.getUser().getEmail().equals(user.getEmail())) {
            user.setPassword(this.passwordEncoder.encode(request.get("password")));
            this.userRepository.save(user);
            this.iActivationRepository.delete(savedActivation);
            this.notificationService.sendPasswordChangedConfirmationMail(user);
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
            .findByEmailWithSchoolScopes(username)
            .orElseThrow(() -> new UsernameNotFoundException("Pas d'utilisateur pour cet identifiant"));
    }

    public User getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    /**
     * Met à jour le mot de passe du compte connecté après vérification de l’ancien.
     */
    @Transactional
    public void changeOwnPassword(User principal, String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe actuel requis.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nouveau mot de passe requis.");
        }
        if (newPassword.equals(currentPassword)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Le nouveau mot de passe doit être différent du mot de passe actuel."
            );
        }
        User user = this.userRepository.findById(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        if (!this.passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe actuel est incorrect.");
        }
        user.setPassword(this.passwordEncoder.encode(newPassword));
        this.userRepository.save(user);
        this.notificationService.sendPasswordChangedConfirmationMail(user);
    }

    private Activation createActivation(User user) {
        if (user.getId() != null) {
            this.iActivationRepository.deleteAllByUser_Id(user.getId());
        }
        Random random = new Random();
        Activation activation = new Activation();
        activation.setCode(String.format("%06d", random.nextInt(999999)));
        activation.setRegistrationDate(Instant.now());
        activation.setExpiration(
            Instant.now().plusMillis(NotificationService.ACTIVATION_CODE_VALIDITY_MINUTES * 60L * 1000L)
        );
        activation.setUser(user);
        return iActivationRepository.save(activation);
    }

    /**
     * Même périmètre que {@link #getAll()} : qui peut voir l’utilisateur peut lui renvoyer un code d’activation.
     */
    private void assertCurrentUserMayManageTargetUser(User current, User target) {
        if (target.getRole() == User.UserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action interdite.");
        }
        if (current.getRole() == User.UserRole.SUPER_ADMIN) {
            return;
        }
        if (current.getRole() == User.UserRole.DIRECTOR) {
            if (target.getRole() != User.UserRole.TEACHER && target.getRole() != User.UserRole.ACCOUNTANT) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action non autorisée.");
            }
            School dirSchool = current.getSchool();
            School tSchool = target.getSchool();
            if (dirSchool == null || dirSchool.getId() == null || tSchool == null || tSchool.getId() == null
                || !dirSchool.getId().equals(tSchool.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action non autorisée.");
            }
            schoolService.assertCurrentUserCanAccessSchool(dirSchool.getId());
            return;
        }
        if (current.getRole() == User.UserRole.ADMIN_ECOLE) {
            Long tenantId = current.getOrganizationTenantId() != null
                ? current.getOrganizationTenantId()
                : current.getTenantId();
            Long uTenant = target.getOrganizationTenantId() != null
                ? target.getOrganizationTenantId()
                : target.getTenantId();
            if (tenantId == null || uTenant == null || !tenantId.equals(uTenant)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action non autorisée.");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action non autorisée.");
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
