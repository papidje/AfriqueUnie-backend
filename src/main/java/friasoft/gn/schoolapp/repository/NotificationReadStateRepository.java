package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.notification.NotificationReadState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationReadStateRepository extends JpaRepository<NotificationReadState, Long> {

    boolean existsByNotification_IdAndUser_Id(Long notificationId, Long userId);

    Optional<NotificationReadState> findByNotification_IdAndUser_Id(Long notificationId, Long userId);

    @Query(
        """
            SELECT r FROM NotificationReadState r JOIN FETCH r.notification
            WHERE r.user.id = :userId AND r.notification.id IN :ids
            """
    )
    List<NotificationReadState> findByUser_IdAndNotification_IdIn(
        @Param("userId") Long userId,
        @Param("ids") Collection<Long> ids
    );
}
