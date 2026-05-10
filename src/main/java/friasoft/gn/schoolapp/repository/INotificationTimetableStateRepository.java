package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.NotificationTimetableState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface INotificationTimetableStateRepository extends JpaRepository<NotificationTimetableState, Long> {

    Optional<NotificationTimetableState> findByTenantIdAndSchoolClass_Id(Long tenantId, Long schoolClassId);
}
