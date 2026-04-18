package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>{

    Optional<User> findByEmail(String email);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.school
        WHERE u.email = :email
        """)
    Optional<User> findByEmailWithSchoolScopes(@Param("email") String email);
    List<User> findAllBySchoolId(Long schoolId);
    @Query("""
        SELECT u FROM User u
        WHERE (LOWER(u.fullname) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%')))
        AND u.school IS NULL
    """)
    List<User> searchByKeywordAndNoSchool(@Param("term") String keyword);

    @Query("""
        SELECT u FROM User u
        WHERE u.role = friasoft.gn.schoolapp.entity.auth.User$UserRole.TEACHER
        AND u.isActive = true
        AND (
            (:schoolTenantId IS NOT NULL AND u.tenantId IS NOT NULL AND u.tenantId = :schoolTenantId)
            OR (u.school IS NOT NULL AND u.school.id = :schoolId)
        )
        ORDER BY u.fullname
        """)
    List<User> findTeachersForSchool(@Param("schoolId") Long schoolId, @Param("schoolTenantId") Long schoolTenantId);

    @Query("""
        SELECT COUNT(u) FROM User u
        WHERE u.id = :userId
        AND u.role = friasoft.gn.schoolapp.entity.auth.User$UserRole.TEACHER
        AND u.isActive = true
        AND (
            (:schoolTenantId IS NOT NULL AND u.tenantId IS NOT NULL AND u.tenantId = :schoolTenantId)
            OR (u.school IS NOT NULL AND u.school.id = :schoolId)
        )
        """)
    long countTeacherAssignableToSchool(
        @Param("userId") Long userId,
        @Param("schoolId") Long schoolId,
        @Param("schoolTenantId") Long schoolTenantId
    );

    /** {@code school_id} : directeur, staff, enseignant, comptable ; absent pour admin organisation / super admin. */
    @Query("select u.school.id from User u where u.id = :userId and u.school is not null")
    Optional<Long> findSchoolIdByUserId(@Param("userId") Long userId);
}
