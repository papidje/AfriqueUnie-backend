package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.RegistrationRequest;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.tenant.Tenant;
import friasoft.gn.schoolapp.entity.auth.UserPlatformRole;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import friasoft.gn.schoolapp.repository.TenantRepository;
import friasoft.gn.schoolapp.repository.UserPlatformRoleRepository;
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
    private final UserPlatformRoleRepository userPlatformRoleRepository;
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public User registerSchoolAdmin(RegistrationRequest request) {
        validateRequest(request);
        userRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new RuntimeException("Email deja utilisé");
        });

        String fn = trimToNull(request.adminFirstName());
        String ln = trimToNull(request.adminLastName());
        String schoolAddressNorm = normalizeBlankToNull(request.schoolAddress());

        Tenant tenant = new Tenant();
        tenant.setName(request.tenantName());
        tenant.setAddress(schoolAddressNorm);
        tenant.setLogo(request.tenantLogo());
        tenant = tenantRepository.save(tenant);

        String resolvedSchoolName = resolveSchoolName(request);
        School school = new School();
        school.setTenantId(tenant.getId());
        school.setName(resolvedSchoolName);
        school.setAdress(schoolAddressNorm);
        school.setContact(normalizeBlankToNull(request.schoolContact()));
        school.setLogo(request.tenantLogo());
        school.setActive(false);
        school.setCreated_at(Instant.now());
        school = schoolRepository.save(school);

        User user = new User();
        user.setUsername(request.username() != null ? request.username() : request.email());
        user.setFirstName(fn);
        user.setLastName(ln);
        user.setFullname(composeFullName(fn, ln));
        user.setEmail(request.email());
        user.setTenantId(tenant.getId());
        user.setSchool(school);
        // Mot de passe initial technique: le vrai mot de passe est défini à l’activation.
        user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setActive(false);

        User saved = userRepository.save(user);
        UserPlatformRole pr = new UserPlatformRole();
        pr.setUser(saved);
        pr.setRole(User.UserRole.ADMIN_ECOLE);
        userPlatformRoleRepository.save(pr);
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
        if (request.tenantName() == null || request.tenantName().isBlank()) {
            throw new RuntimeException("Nom du tenant obligatoire");
        }
        if (request.schoolAddress() == null || request.schoolAddress().isBlank()) {
            throw new RuntimeException("Adresse de l'établissement obligatoire");
        }
        if (request.schoolContact() == null || request.schoolContact().isBlank()) {
            throw new RuntimeException("Téléphone obligatoire");
        }
        if (trimToNull(request.adminFirstName()) == null) {
            throw new RuntimeException("Prénom de l'administrateur obligatoire");
        }
        if (trimToNull(request.adminLastName()) == null) {
            throw new RuntimeException("Nom de l'administrateur obligatoire");
        }
    }

    private static String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    /** Chaîne non vide après trim, sinon {@code null}. */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static String composeFullName(String firstName, String lastName) {
        String fn = firstName == null ? "" : firstName.trim();
        String ln = lastName == null ? "" : lastName.trim();
        if (fn.isEmpty()) {
            return ln;
        }
        if (ln.isEmpty()) {
            return fn;
        }
        return fn + " " + ln;
    }
}
