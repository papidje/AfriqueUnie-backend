package friasoft.gn.schoolapp.security;

import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.auth.UserPlatformRole;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import friasoft.gn.schoolapp.repository.UserPlatformRoleRepository;
import friasoft.gn.schoolapp.repository.UserSchoolAffiliationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("schoolSecurity")
@RequiredArgsConstructor
public class SchoolSecurity {

    private final SchoolRepository schoolRepository;
    private final UserSchoolAffiliationRepository userSchoolAffiliationRepository;
    private final UserPlatformRoleRepository userPlatformRoleRepository;

    public boolean checkUserSchool(Authentication auth, Long schoolId) {
        User user = (User) auth.getPrincipal();
        return schoolRepository.findById(schoolId)
            .map(s -> canAccessSchool(user, s))
            .orElse(false);
    }

    /**
     * ADMIN_ECOLE : tous les établissements du tenant organisation.
     * Autres profils : affiliation active pour cette école, sinon repli sur {@link User#getSchool()} (legacy).
     */
    public boolean canAccessSchool(User user, School school) {
        if (isSuperAdmin(user)) {
            return false;
        }
        if (isAdminEcole(user)) {
            Long orgTid = user.getOrganizationTenantId();
            if (orgTid == null) {
                orgTid = user.getTenantId();
            }
            return orgTid != null && orgTid.equals(school.getTenantId());
        }
        if (userSchoolAffiliationRepository.countActiveByUserIdAndSchoolId(user.getId(), school.getId()) > 0) {
            return true;
        }
        /*
         * Affiliation explicite mais inactive (suspension ou invitation non acceptée) : ne pas retomber sur
         * {@code users.school_id}, sinon le JWT pourrait encore cibler l’établissement après suspension.
         */
        if (userSchoolAffiliationRepository.countByUserIdAndSchoolId(user.getId(), school.getId()) > 0) {
            return false;
        }
        return user.getSchool() != null && user.getSchool().getId().equals(school.getId());
    }

    private boolean isSuperAdmin(User user) {
        return userPlatformRoleRepository.findByUser_Id(user.getId())
            .map(UserPlatformRole::getRole)
            .filter(r -> r == User.UserRole.SUPER_ADMIN)
            .isPresent();
    }

    private boolean isAdminEcole(User user) {
        return userPlatformRoleRepository.findByUser_Id(user.getId())
            .map(UserPlatformRole::getRole)
            .filter(r -> r == User.UserRole.ADMIN_ECOLE)
            .isPresent();
    }
}
