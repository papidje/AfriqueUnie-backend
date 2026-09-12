package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.auth.UserPlatformRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserPlatformRoleRepository extends JpaRepository<UserPlatformRole, Long> {

    Optional<UserPlatformRole> findByUser_Id(Long userId);

    Optional<UserPlatformRole> findByUser(User user);

    boolean existsByUser_IdAndRole(Long userId, User.UserRole role);

    @Query("""
        SELECT pr.user FROM UserPlatformRole pr
        WHERE pr.role = :role AND pr.user.isActive = true
        ORDER BY pr.user.fullname ASC
        """)
    List<User> findActiveUsersByRole(@Param("role") User.UserRole role);
}
