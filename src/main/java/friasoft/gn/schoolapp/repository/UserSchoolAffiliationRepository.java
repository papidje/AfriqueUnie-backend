package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.auth.UserSchoolAffiliation;
import friasoft.gn.schoolapp.entity.school.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserSchoolAffiliationRepository extends JpaRepository<UserSchoolAffiliation, Long> {

    List<UserSchoolAffiliation> findAllByUser_Id(Long userId);

    @Query(
        """
            SELECT DISTINCT a FROM UserSchoolAffiliation a JOIN FETCH a.school s
            WHERE a.user.id = :userId
            """
    )
    List<UserSchoolAffiliation> findAllByUser_IdJoinSchool(@Param("userId") Long userId);

    List<UserSchoolAffiliation> findAllBySchool_Id(Long schoolId);

    Optional<UserSchoolAffiliation> findByUser_IdAndSchool_IdAndRole(
        Long userId,
        Long schoolId,
        User.UserRole role
    );

    List<UserSchoolAffiliation> findAllByUser_IdAndSchool_Id(Long userId, Long schoolId);

    List<UserSchoolAffiliation> findAllByUser_IdAndSchool_IdAndActiveTrue(Long userId, Long schoolId);

    boolean existsByUser_IdAndSchool_IdAndActiveTrue(Long userId, Long schoolId);

    /**
     * Rattachement (y compris invitation en attente / suspendu) vers une école de ce tenant organisation.
     */
    boolean existsByUser_IdAndSchool_TenantId(Long userId, Long tenantId);

    /**
     * Compte les affiliations actives sans passer par le filtre Hibernate tenant (accès cross-tenant légitime).
     */
    @Query(
        value = """
            SELECT COUNT(*) FROM schools.user_school_affiliations
            WHERE user_id = :userId AND school_id = :schoolId AND is_active = true
            """,
        nativeQuery = true
    )
    long countActiveByUserIdAndSchoolId(@Param("userId") Long userId, @Param("schoolId") Long schoolId);

    /**
     * Lignes d’affiliation (actives ou non) pour la paire utilisateur / école — hors filtre tenant.
     */
    @Query(
        value = """
            SELECT COUNT(*) FROM schools.user_school_affiliations
            WHERE user_id = :userId AND school_id = :schoolId
            """,
        nativeQuery = true
    )
    long countByUserIdAndSchoolId(@Param("userId") Long userId, @Param("schoolId") Long schoolId);

    /**
     * Rôles d’affiliation actifs pour une paire utilisateur / école, hors filtre tenant.
     */
    @Query(
        value = """
            SELECT DISTINCT role FROM schools.user_school_affiliations
            WHERE user_id = :userId AND school_id = :schoolId AND is_active = true
            """,
        nativeQuery = true
    )
    List<String> findActiveRoleNamesForUserAndSchool(@Param("userId") Long userId, @Param("schoolId") Long schoolId);

    boolean existsByUser_IdAndSchool_IdAndActiveTrueAndRoleIn(
        Long userId,
        Long schoolId,
        Collection<User.UserRole> roles
    );

    @Query(
        """
            SELECT a FROM UserSchoolAffiliation a JOIN FETCH a.school s
            WHERE a.user.id IN :userIds AND a.active = true
            ORDER BY a.user.id ASC, s.id ASC
            """
    )
    List<UserSchoolAffiliation> findAllActiveWithSchoolByUser_IdIn(@Param("userIds") Collection<Long> userIds);

    /**
     * Rattachements (actifs ou non) vers des écoles du tenant — annuaire / masquage confidentialité.
     */
    @Query(
        """
            SELECT a FROM UserSchoolAffiliation a
            JOIN FETCH a.school s
            JOIN FETCH a.user u
            WHERE u.id IN :userIds AND s.tenantId = :tenantId
            """
    )
    List<UserSchoolAffiliation> findAllWithSchoolByUser_IdInAndTenantId(
        @Param("userIds") Collection<Long> userIds,
        @Param("tenantId") Long tenantId
    );

    @Query(
        "SELECT a.school FROM UserSchoolAffiliation a WHERE a.user.id = :userId AND a.active = true ORDER BY a.school.id ASC"
    )
    List<School> findActiveSchoolsForUser(@Param("userId") Long userId);

    @Query(
        """
            SELECT a FROM UserSchoolAffiliation a JOIN FETCH a.school s
            WHERE a.id = :id AND a.user.id = :userId
            """
    )
    Optional<UserSchoolAffiliation> findByIdAndUser_IdFetchSchool(@Param("id") Long id, @Param("userId") Long userId);
}
