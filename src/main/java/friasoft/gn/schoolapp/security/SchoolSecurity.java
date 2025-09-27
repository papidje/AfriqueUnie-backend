package friasoft.gn.schoolapp.security;

import friasoft.gn.schoolapp.entity.User;
import friasoft.gn.schoolapp.enums.RoleEnum;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("schoolSecurity")
public class SchoolSecurity {
    public boolean checkUserSchool(Authentication auth, Long schoolId) {
        User user = (User) auth.getPrincipal();
        return user.getRoles().stream()
            .anyMatch(r -> r.getName().equals(RoleEnum.SUPER_ADMIN))
            || (user.getSchool() != null && schoolId.equals(user.getSchool().getId()));
    }
}
