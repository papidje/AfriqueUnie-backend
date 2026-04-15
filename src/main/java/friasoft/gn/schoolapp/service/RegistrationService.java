package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.RegistrationRequest;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.tenant.Tenant;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import friasoft.gn.schoolapp.repository.TenantRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final TenantRepository tenantRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public User registerSchoolAdmin(RegistrationRequest request) {
        validateRequest(request);
        userRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new RuntimeException("Email deja utilisé");
        });

        Tenant tenant = new Tenant();
        tenant.setName(request.tenantName());
        tenant.setAddress(request.tenantAddress());
        tenant.setLogo(request.tenantLogo());
        tenant = tenantRepository.save(tenant);

        String resolvedSchoolName = resolveSchoolName(request);
        School school = new School();
        school.setTenantId(tenant.getId());
        school.setName(resolvedSchoolName);
        school.setAdress(request.tenantAddress());
        school.setLogo(request.tenantLogo());
        school.setActive(false);
        school.setCreated_at(Instant.now());
        school = schoolRepository.save(school);

        User user = new User();
        user.setUsername(request.username() != null ? request.username() : request.email());
        user.setFullname(request.fullname());
        user.setEmail(request.email());
        user.setRole(User.UserRole.ADMIN_ECOLE);
        user.setTenantId(tenant.getId());
        user.setSchool(school);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setActive(false);

        User saved = userRepository.save(user);
        userService.sendActivationEmail(saved);
        return saved;
    }

    private static String resolveSchoolName(RegistrationRequest request) {
        if (request.schoolName() != null && !request.schoolName().isBlank()) {
            return request.schoolName().trim();
        }
        return request.tenantName().trim();
    }

    private void validateRequest(RegistrationRequest request) {
        if (request.email() == null || !request.email().contains("@")) {
            throw new RuntimeException("Email invalide");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new RuntimeException("Mot de passe obligatoire");
        }
        if (request.tenantName() == null || request.tenantName().isBlank()) {
            throw new RuntimeException("Nom du tenant obligatoire");
        }
    }
}
