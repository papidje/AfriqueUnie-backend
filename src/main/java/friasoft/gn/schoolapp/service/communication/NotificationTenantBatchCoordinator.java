package friasoft.gn.schoolapp.service.communication;

import friasoft.gn.schoolapp.entity.school.NotificationBatchSettings;
import friasoft.gn.schoolapp.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationTenantBatchCoordinator {

    private final NotificationBatchSettingsService notificationBatchSettingsService;
    private final NotificationEvaluationReminderJob notificationEvaluationReminderJob;
    private final NotificationPaymentReminderJob notificationPaymentReminderJob;
    private final NotificationTimetableChangeJob notificationTimetableChangeJob;

    @Transactional
    public void executeJobsForCurrentTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return;
        }
        NotificationBatchSettings settings = notificationBatchSettingsService.resolveForTenant(tenantId);
        notificationEvaluationReminderJob.run(settings);
        notificationPaymentReminderJob.run(settings);
        notificationTimetableChangeJob.run(settings);
    }
}
