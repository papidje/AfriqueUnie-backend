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
        if (user.getRole() == User.UserRole.SUPER_ADMIN) {
            return true;
        }
        return schoolRepository.findById(schoolId)
            .map(s -> schoolAccessibleByUser(user, s))
            .orElse(false);
    }

    private static boolean schoolAccessibleByUser(User user, School school) {
        if (user.getTenantId() != null && user.getTenantId().equals(school.getTenantId())) {
            return true;
        }
        return user.getSchool() != null && user.getSchool().getId().equals(school.getId());
    }
}
