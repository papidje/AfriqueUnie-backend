package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.ActivationRequest;
import friasoft.gn.schoolapp.dto.UserRequest;
import friasoft.gn.schoolapp.entity.auth.Activation;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.repository.IActivationRepository;
import friasoft.gn.schoolapp.repository.SchoolRepository;
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

import java.time.Instant;
import java.util.*;

@Slf4j
@AllArgsConstructor
@Service
public class UserService implements UserDetailsService{

    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private IActivationRepository iActivationRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private NotificationService notificationService;

    public void registery(UserRequest userInput) {
        if (!userInput.email().contains("@")) {
            throw new RuntimeException("Email invalide");
        }
        Optional<User> userOptional = this.userRepository.findByEmail(userInput.email());
        if (userOptional.isPresent()) {
            throw new RuntimeException("Email deja utilisé");
        }


        User user = new User();
        user.setUsername(userInput.username());
        user.setEmail(userInput.email());
        user.setFullname(userInput.fullname());

        if (userInput.schoolId() != null) {
            School school = schoolRepository.findById(userInput.schoolId())
                .orElseThrow(() -> new RuntimeException("Ecole inconnu"));
            user.setSchool(school);
            user.setTenantId(school.getId());
        }
        user.setRole(resolveRole(userInput));
        user.setPassword(this.passwordEncoder.encode(userInput.password()));
        user = this.userRepository.save(user);

        Activation activation = createActivation(user);
        this.notificationService.sendActivationMail(activation);
    }

    public void sendActivationEmail(User user) {
        Activation activation = createActivation(user);
        this.notificationService.sendActivationMail(activation);
    }

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE', 'SUPER_ADMIN')")
    public List<User> getAll() {
        List<User> actualList = new ArrayList<>();
        this.userRepository.findAll().iterator().forEachRemaining(actualList::add);
        return actualList;
    }

    public void activate(ActivationRequest activation) {
        Activation savedActivation = this.iActivationRepository.findByCode(activation.code())
            .orElseThrow(() -> new RuntimeException("Activation code invalide"));
        User user = this.userRepository.findByEmail(activation.userMail()).orElseThrow();
        if(Instant.now().isBefore(savedActivation.getExpiration()) && savedActivation.getUser().getEmail().equals(user.getEmail())) {
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

    private User.UserRole resolveRole(UserRequest userInput) {
        if (userInput.role() != null) {
            return userInput.role();
        }
        return userInput.schoolId() != null ? User.UserRole.ADMIN_ECOLE : User.UserRole.STAFF;
    }
}
