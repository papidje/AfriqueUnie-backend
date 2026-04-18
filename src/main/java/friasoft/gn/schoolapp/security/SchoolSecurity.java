package friasoft.gn.schoolapp.security;

import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("schoolSecurity")
@RequiredArgsConstructor
public class SchoolSecurity {

    private final SchoolRepository schoolRepository;

    public boolean checkUserSchool(Authentication auth, Long schoolId) {
        User user = (User) auth.getPrincipal();
        return schoolRepository.findById(schoolId)
            .map(s -> canAccessSchool(user, s))
            .orElse(false);
    }

    /**
     * ADMIN_ECOLE : tout établissement du même tenant (seul profil « multi-écoles » côté accès).
     * DIRECTOR / STAFF / TEACHER / ACCOUNTANT : uniquement {@link User#getSchool()} lorsqu’il est renseigné.
     */
    public boolean canAccessSchool(User user, School school) {
        if (user.getRole() == User.UserRole.SUPER_ADMIN) {
            return true;
        }
        if (user.getRole() == User.UserRole.DIRECTOR) {
            return user.getSchool() != null && user.getSchool().getId().equals(school.getId());
        }
        if (user.getRole() == User.UserRole.ADMIN_ECOLE) {
            Long orgTid = user.getOrganizationTenantId();
            return orgTid != null && orgTid.equals(school.getTenantId());
        }
        if (user.getRole() == User.UserRole.STAFF
            || user.getRole() == User.UserRole.TEACHER
            || user.getRole() == User.UserRole.ACCOUNTANT) {
            return user.getSchool() != null && user.getSchool().getId().equals(school.getId());
        }
        return false;
    }
}
