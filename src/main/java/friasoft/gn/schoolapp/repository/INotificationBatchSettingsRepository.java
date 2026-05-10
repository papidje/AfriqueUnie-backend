package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.NotificationBatchSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface INotificationBatchSettingsRepository extends JpaRepository<NotificationBatchSettings, Long> {

    Optional<NotificationBatchSettings> findByTenantId(Long tenantId);
}
