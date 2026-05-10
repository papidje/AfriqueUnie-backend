package friasoft.gn.schoolapp.service.communication;

import friasoft.gn.schoolapp.entity.tenant.Tenant;
import friasoft.gn.schoolapp.repository.TenantRepository;
import friasoft.gn.schoolapp.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationBatchOrchestrator {

    private final TenantRepository tenantRepository;
    private final NotificationTenantBatchCoordinator notificationTenantBatchCoordinator;

    @Value("${application.communication.scheduled-enabled:true}")
    private boolean scheduledEnabled;

    /**
     * Exécute les trois familles de jobs pour chaque tenant (filtre Hibernate activé par {@link TenantContext}).
     */
    @Scheduled(cron = "${application.communication.batch-cron:0 20 7 * * *}")
    public void runDailyBatch() {
        if (!scheduledEnabled) {
            return;
        }
        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getId() == null) {
                continue;
            }
            TenantContext.setTenantId(tenant.getId());
            try {
                notificationTenantBatchCoordinator.executeJobsForCurrentTenant();
            } catch (Exception e) {
                log.warn("Notification batch tenant {} : {}", tenant.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
