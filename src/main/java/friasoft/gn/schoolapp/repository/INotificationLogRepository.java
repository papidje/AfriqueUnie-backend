package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface INotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    boolean existsBySchoolIdAndEventTypeAndReferenceIdAndParentId(
        Long schoolId,
        String eventType,
        Long referenceId,
        Long parentId
    );
}
