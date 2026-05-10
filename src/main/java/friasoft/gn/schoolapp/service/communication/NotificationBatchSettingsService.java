package friasoft.gn.schoolapp.service.communication;

import friasoft.gn.schoolapp.entity.school.NotificationBatchSettings;
import friasoft.gn.schoolapp.repository.INotificationBatchSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationBatchSettingsService {

    private final INotificationBatchSettingsRepository notificationBatchSettingsRepository;

    @Transactional
    public NotificationBatchSettings resolveForTenant(Long tenantId) {
        return notificationBatchSettingsRepository.findByTenantId(tenantId).orElseGet(() -> createDefaults(tenantId));
    }

    @Transactional
    public NotificationBatchSettings update(Long tenantId, NotificationBatchSettings incoming) {
        NotificationBatchSettings existing = resolveForTenant(tenantId);
        existing.setEvaluationReminderDaysBefore(clampDays(incoming.getEvaluationReminderDaysBefore()));
        existing.setEvaluationReminderEnabled(Boolean.TRUE.equals(incoming.getEvaluationReminderEnabled()));
        existing.setPaymentReminderEnabled(Boolean.TRUE.equals(incoming.getPaymentReminderEnabled()));
        existing.setTimetableChangeEnabled(Boolean.TRUE.equals(incoming.getTimetableChangeEnabled()));
        existing.setBatchChunkSize(clampChunk(incoming.getBatchChunkSize()));
        existing.setEmailEnabled(Boolean.TRUE.equals(incoming.getEmailEnabled()));
        existing.setSmsEnabled(Boolean.TRUE.equals(incoming.getSmsEnabled()));
        return notificationBatchSettingsRepository.save(existing);
    }

    @Transactional
    public NotificationBatchSettings save(NotificationBatchSettings entity) {
        return notificationBatchSettingsRepository.save(entity);
    }

    private NotificationBatchSettings createDefaults(Long tenantId) {
        NotificationBatchSettings s = NotificationBatchSettings.builder()
            .tenantId(tenantId)
            .evaluationReminderDaysBefore(3)
            .evaluationReminderEnabled(true)
            .paymentReminderEnabled(true)
            .timetableChangeEnabled(true)
            .batchChunkSize(50)
            .emailEnabled(true)
            .smsEnabled(false)
            .build();
        return notificationBatchSettingsRepository.save(s);
    }

    private static int clampDays(Integer v) {
        if (v == null) {
            return 3;
        }
        return Math.max(1, Math.min(v, 30));
    }

    private static int clampChunk(Integer v) {
        if (v == null) {
            return 50;
        }
        return Math.max(10, Math.min(v, 500));
    }
}
