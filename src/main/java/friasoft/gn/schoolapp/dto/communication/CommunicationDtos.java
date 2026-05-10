package friasoft.gn.schoolapp.dto.communication;

import java.time.Instant;
import java.util.List;

public final class CommunicationDtos {

    private CommunicationDtos() {
    }

    public record CommunicationBatchSettingsResponse(
        int evaluationReminderDaysBefore,
        boolean evaluationReminderEnabled,
        boolean paymentReminderEnabled,
        boolean timetableChangeEnabled,
        int batchChunkSize,
        boolean emailEnabled,
        boolean smsEnabled
    ) {
    }

    public record CommunicationBatchSettingsUpdateRequest(
        Integer evaluationReminderDaysBefore,
        Boolean evaluationReminderEnabled,
        Boolean paymentReminderEnabled,
        Boolean timetableChangeEnabled,
        Integer batchChunkSize,
        Boolean emailEnabled,
        Boolean smsEnabled
    ) {
    }

    public record CommunicationScheduledPreviewRow(
        String kind,
        String label,
        long estimatedNotifications,
        String detail
    ) {
    }

    public record CommunicationManualSendRequest(
        Long schoolId,
        String title,
        String message,
        String channel,
        List<Long> schoolClassIds
    ) {
    }

    public record CommunicationManualSendResponse(int attempted, int successes, int failures, int skippedDuplicates) {
    }

    public record CommunicationHistoryRow(
        long id,
        Instant createdAt,
        String source,
        String eventType,
        String status,
        String channel,
        String title,
        String recipientsSummary,
        String bodyPreview,
        String bodyContent,
        String errorMessage
    ) {
    }
}
