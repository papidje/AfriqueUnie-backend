package friasoft.gn.schoolapp.service.communication;

import friasoft.gn.schoolapp.entity.school.NotificationDeliveryHistory;
import friasoft.gn.schoolapp.entity.school.NotificationLog;
import friasoft.gn.schoolapp.repository.INotificationDeliveryHistoryRepository;
import friasoft.gn.schoolapp.repository.INotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationOutcomeRecorder {

    private static final int MAX_BODY_CONTENT = 262_144;

    private final INotificationLogRepository notificationLogRepository;
    private final INotificationDeliveryHistoryRepository notificationDeliveryHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(
        Long tenantId,
        Long schoolId,
        String eventType,
        Long referenceId,
        Long parentDedupeId,
        NotificationDeliveryHistory.Channel channel,
        String recipientsSummary,
        String title,
        String bodyPreview,
        String bodyContent
    ) {
        Objects.requireNonNull(schoolId, "schoolId");
        notificationLogRepository.save(
            NotificationLog.builder()
                .tenantId(tenantId)
                .schoolId(schoolId)
                .eventType(eventType)
                .referenceId(referenceId)
                .parentId(parentDedupeId)
                .sentAt(Instant.now())
                .build()
        );
        notificationDeliveryHistoryRepository.save(
            NotificationDeliveryHistory.builder()
                .tenantId(tenantId)
                .schoolId(schoolId)
                .source(NotificationDeliveryHistory.Source.BATCH)
                .eventType(eventType)
                .referenceId(referenceId)
                .parentId(parentDedupeId)
                .status(NotificationDeliveryHistory.Status.SUCCESS)
                .channel(channel)
                .recipientsSummary(truncate(recipientsSummary, 1990))
                .title(truncate(title, 298))
                .bodyPreview(truncate(bodyPreview, 3990))
                .bodyContent(truncate(bodyContent, MAX_BODY_CONTENT))
                .createdAt(Instant.now())
                .build()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
        Long tenantId,
        Long schoolId,
        String eventType,
        Long referenceId,
        Long parentDedupeId,
        NotificationDeliveryHistory.Channel channel,
        String recipientsSummary,
        String title,
        String bodyPreview,
        String bodyContent,
        String errorMessage
    ) {
        Objects.requireNonNull(schoolId, "schoolId");
        notificationDeliveryHistoryRepository.save(
            NotificationDeliveryHistory.builder()
                .tenantId(tenantId)
                .schoolId(schoolId)
                .source(NotificationDeliveryHistory.Source.BATCH)
                .eventType(eventType)
                .referenceId(referenceId)
                .parentId(parentDedupeId)
                .status(NotificationDeliveryHistory.Status.FAILURE)
                .channel(channel)
                .recipientsSummary(truncate(recipientsSummary, 1990))
                .title(truncate(title, 298))
                .bodyPreview(truncate(bodyPreview, 3990))
                .bodyContent(truncate(bodyContent, MAX_BODY_CONTENT))
                .errorMessage(truncate(errorMessage, 1990))
                .createdAt(Instant.now())
                .build()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordManualSuccess(
        Long tenantId,
        Long schoolId,
        String eventType,
        NotificationDeliveryHistory.Channel channel,
        String recipientsSummary,
        String title,
        String bodyPreview,
        String bodyContent
    ) {
        Objects.requireNonNull(schoolId, "schoolId");
        notificationDeliveryHistoryRepository.save(
            NotificationDeliveryHistory.builder()
                .tenantId(tenantId)
                .schoolId(schoolId)
                .source(NotificationDeliveryHistory.Source.MANUAL)
                .eventType(eventType)
                .referenceId(null)
                .parentId(null)
                .status(NotificationDeliveryHistory.Status.SUCCESS)
                .channel(channel)
                .recipientsSummary(truncate(recipientsSummary, 1990))
                .title(truncate(title, 298))
                .bodyPreview(truncate(bodyPreview, 3990))
                .bodyContent(truncate(bodyContent, MAX_BODY_CONTENT))
                .createdAt(Instant.now())
                .build()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordManualFailure(
        Long tenantId,
        Long schoolId,
        String eventType,
        NotificationDeliveryHistory.Channel channel,
        String recipientsSummary,
        String title,
        String bodyPreview,
        String bodyContent,
        String errorMessage
    ) {
        Objects.requireNonNull(schoolId, "schoolId");
        notificationDeliveryHistoryRepository.save(
            NotificationDeliveryHistory.builder()
                .tenantId(tenantId)
                .schoolId(schoolId)
                .source(NotificationDeliveryHistory.Source.MANUAL)
                .eventType(eventType)
                .referenceId(null)
                .parentId(null)
                .status(NotificationDeliveryHistory.Status.FAILURE)
                .channel(channel)
                .recipientsSummary(truncate(recipientsSummary, 1990))
                .title(truncate(title, 298))
                .bodyPreview(truncate(bodyPreview, 3990))
                .bodyContent(truncate(bodyContent, MAX_BODY_CONTENT))
                .errorMessage(truncate(errorMessage, 1990))
                .createdAt(Instant.now())
                .build()
        );
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
