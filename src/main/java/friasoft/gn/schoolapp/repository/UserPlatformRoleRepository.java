package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.auth.UserPlatformRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPlatformRoleRepository extends JpaRepository<UserPlatformRole, Long> {

    Optional<UserPlatformRole> findByUser_Id(Long userId);

    Optional<UserPlatformRole> findByUser(User user);

    boolean existsByUser_IdAndRole(Long userId, User.UserRole role);
}
