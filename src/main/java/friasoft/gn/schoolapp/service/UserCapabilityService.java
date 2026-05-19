package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.auth.UserPlatformRole;
import friasoft.gn.schoolapp.entity.auth.UserSchoolAffiliation;
import friasoft.gn.schoolapp.repository.UserPlatformRoleRepository;
import friasoft.gn.schoolapp.repository.UserSchoolAffiliationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Capacités dérivées des rôles plateforme et des affiliations actives (plus de {@code users.role}).
 */
@Service
@RequiredArgsConstructor
public class UserCapabilityService {

    private final UserPlatformRoleRepository userPlatformRoleRepository;
    private final UserSchoolAffiliationRepository userSchoolAffiliationRepository;

    public Optional<User.UserRole> platformRole(User user) {
        if (user == null || user.getId() == null) {
            return Optional.empty();
        }
        return userPlatformRoleRepository.findByUser_Id(user.getId()).map(UserPlatformRole::getRole);
    }

    public boolean isSuperAdmin(User user) {
        return platformRole(user).filter(r -> r == User.UserRole.SUPER_ADMIN).isPresent();
    }

    public boolean isAdminEcole(User user) {
        return platformRole(user).filter(r -> r == User.UserRole.ADMIN_ECOLE).isPresent();
    }

    public boolean hasActiveAffiliationRole(User user, Long schoolId, User.UserRole role) {
        if (user == null || user.getId() == null || schoolId == null || role == null) {
            return false;
        }
        return userSchoolAffiliationRepository
            .findByUser_IdAndSchool_IdAndRole(user.getId(), schoolId, role)
            .map(UserSchoolAffiliation::isActive)
            .orElse(false);
    }

    public boolean hasActiveTeacherAtSchool(User user, Long schoolId) {
        return hasActiveAffiliationRole(user, schoolId, User.UserRole.TEACHER);
    }

    public boolean isDirectorAtSchool(User user, Long schoolId) {
        return hasActiveAffiliationRole(user, schoolId, User.UserRole.DIRECTOR);
    }

    /** Contexte « annuaire restreint » : directeur sur son école principale. */
    public boolean isDirectorPrimarySchool(User user) {
        if (user.getSchool() == null || user.getSchool().getId() == null) {
            return false;
        }
        return isDirectorAtSchool(user, user.getSchool().getId());
    }

    public boolean hasAnyActiveSchoolScopedRole(User user, Long schoolId, User.UserRole... roles) {
        for (User.UserRole r : roles) {
            if (hasActiveAffiliationRole(user, schoolId, r)) {
                return true;
            }
        }
        return false;
    }
}
