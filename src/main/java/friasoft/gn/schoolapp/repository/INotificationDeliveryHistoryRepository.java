package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.NotificationDeliveryHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface INotificationDeliveryHistoryRepository extends JpaRepository<NotificationDeliveryHistory, Long> {

    Page<NotificationDeliveryHistory> findBySchoolIdOrderByCreatedAtDesc(Long schoolId, Pageable pageable);
}
