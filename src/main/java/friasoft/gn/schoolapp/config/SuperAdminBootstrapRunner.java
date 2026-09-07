package friasoft.gn.schoolapp.config;

import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.auth.UserPlatformRole;
import friasoft.gn.schoolapp.repository.UserPlatformRoleRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Garantit la présence d’un compte {@code SUPER_ADMIN} (création ou réparation) au démarrage.
 * Pas de tenant / école : rôle plateforme uniquement.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(SuperAdminBootstrapProperties.class)
public class SuperAdminBootstrapRunner implements ApplicationRunner {

    private final SuperAdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final UserPlatformRoleRepository userPlatformRoleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("Bootstrap SUPER_ADMIN désactivé (app.super-admin.enabled=false).");
            return;
        }

        String email = properties.getEmail() == null ? "" : properties.getEmail().trim();
        String password = properties.getPassword();
        if (email.isEmpty() || password == null || password.isBlank()) {
            log.warn("Bootstrap SUPER_ADMIN ignoré : email ou mot de passe vide.");
            return;
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        boolean created = user == null;
        if (created) {
            user = new User();
            user.setEmail(email);
            user.setUsername(email);
            user.setFullname(properties.getFullName());
            user.setActive(true);
            user.setTenantId(null);
            user.setSchool(null);
            user.setCreatedAt(Instant.now());
            user.setUpdatedAt(Instant.now());
            user.setLastLoginAt(Instant.now());
            user.setPassword(passwordEncoder.encode(password));
            user = userRepository.save(user);
            log.info("Compte SUPER_ADMIN créé : {}", email);
        } else {
            user.setEmail(email);
            user.setUsername(email);
            if (properties.getFullName() != null && !properties.getFullName().isBlank()) {
                user.setFullname(properties.getFullName());
            }
            user.setActive(true);
            user.setTenantId(null);
            user.setUpdatedAt(Instant.now());
            if (properties.isResetPasswordOnStartup()) {
                user.setPassword(passwordEncoder.encode(password));
                log.info("Mot de passe SUPER_ADMIN réinitialisé au démarrage pour {}", email);
            }
            user = userRepository.save(user);
        }

        ensurePlatformRole(user);

        if (!created && !properties.isResetPasswordOnStartup()) {
            log.info("Compte SUPER_ADMIN vérifié (rôle + actif) : {}", email);
        }
    }

    private void ensurePlatformRole(User user) {
        UserPlatformRole platformRole = userPlatformRoleRepository.findByUser_Id(user.getId()).orElse(null);
        if (platformRole == null) {
            platformRole = new UserPlatformRole();
            platformRole.setUser(user);
            platformRole.setRole(User.UserRole.SUPER_ADMIN);
            user.setPlatformRole(platformRole);
            userRepository.save(user);
            log.info("Rôle plateforme SUPER_ADMIN ajouté pour {}", user.getEmail());
            return;
        }
        if (platformRole.getRole() != User.UserRole.SUPER_ADMIN) {
            platformRole.setRole(User.UserRole.SUPER_ADMIN);
            userPlatformRoleRepository.save(platformRole);
            log.info("Rôle plateforme mis à jour en SUPER_ADMIN pour {}", user.getEmail());
        }
    }
}
