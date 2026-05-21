package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.notification.NotificationEntity;
import friasoft.gn.schoolapp.entity.notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationEntityRepository extends JpaRepository<NotificationEntity, Long> {

    /**
     * Notifications ciblées et encore affichables : pas d’état lu, ou état avec {@code is_visible = true}.
     * {@code USER_TARGETED} : uniquement {@code target_user_id} — ne pas diffuser via {@code target_school_id}
     * (sinon tout personnel encore actif sur l’établissement voit la suspension d’un collègue).
     */
    @Query(
        """
            SELECT DISTINCT n FROM NotificationEntity n
            LEFT JOIN NotificationReadState r ON r.notification.id = n.id AND r.user.id = :userId
            WHERE (
                n.targetUser.id = :userId
                OR (n.type <> NotificationType.USER_TARGETED AND n.targetSchool IS NOT NULL AND EXISTS (
                    SELECT 1 FROM UserSchoolAffiliation a
                    WHERE a.user.id = :userId AND a.active = true AND a.school.id = n.targetSchool.id
                ))
                OR (n.targetTenant IS NOT NULL AND :tenantId IS NOT NULL AND n.targetTenant.id = :tenantId)
            )
            AND (r.id IS NULL OR r.visible = true)
            ORDER BY n.createdAt DESC
            """
    )
    List<NotificationEntity> findListedForUser(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * Notifications ciblées sans aucune ligne {@code notification_read_states} pour cet utilisateur (pastille non lu).
     */
    @Query(
        """
            SELECT n FROM NotificationEntity n
            WHERE (
                n.targetUser.id = :userId
                OR (n.type <> NotificationType.USER_TARGETED AND n.targetSchool IS NOT NULL AND EXISTS (
                    SELECT 1 FROM UserSchoolAffiliation a
                    WHERE a.user.id = :userId AND a.active = true AND a.school.id = n.targetSchool.id
                ))
                OR (n.targetTenant IS NOT NULL AND :tenantId IS NOT NULL AND n.targetTenant.id = :tenantId)
            )
            AND NOT EXISTS (
                SELECT 1 FROM NotificationReadState r2
                WHERE r2.notification.id = n.id AND r2.user.id = :userId
            )
            ORDER BY n.createdAt DESC
            """
    )
    List<NotificationEntity> findUnreadTargetedForUser(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    List<NotificationEntity> findByLinkIdAndTargetUser_IdAndType(
        Long linkId,
        Long targetUserId,
        NotificationType type
    );

    @Query(
        """
            SELECT COUNT(n) FROM NotificationEntity n
            WHERE n.id = :notificationId
            AND (
                n.targetUser.id = :userId
                OR (n.type <> NotificationType.USER_TARGETED AND n.targetSchool IS NOT NULL AND EXISTS (
                    SELECT 1 FROM UserSchoolAffiliation a
                    WHERE a.user.id = :userId AND a.active = true AND a.school.id = n.targetSchool.id
                ))
                OR (n.targetTenant IS NOT NULL AND :tenantId IS NOT NULL AND n.targetTenant.id = :tenantId)
            )
            """
    )
    long countAccessibleByUser(
        @Param("notificationId") Long notificationId,
        @Param("userId") Long userId,
        @Param("tenantId") Long tenantId
    );

    @Query(
        """
            SELECT COUNT(n) FROM NotificationEntity n
            WHERE (
                n.targetUser.id = :userId
                OR (n.type <> NotificationType.USER_TARGETED AND n.targetSchool IS NOT NULL AND EXISTS (
                    SELECT 1 FROM UserSchoolAffiliation a
                    WHERE a.user.id = :userId AND a.active = true AND a.school.id = n.targetSchool.id
                ))
                OR (n.targetTenant IS NOT NULL AND :tenantId IS NOT NULL AND n.targetTenant.id = :tenantId)
            )
            AND NOT EXISTS (
                SELECT 1 FROM NotificationReadState r
                WHERE r.notification.id = n.id AND r.user.id = :userId
            )
            """
    )
    long countUnreadForUser(@Param("userId") Long userId, @Param("tenantId") Long tenantId);
}
