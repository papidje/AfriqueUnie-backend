package friasoft.gn.schoolapp.dto.messaging;

import java.time.Instant;

public record MessagingConversationDto(
    Long id,
    MessagingUserSummaryDto counterpart,
    String lastMessagePreview,
    Instant lastMessageAt,
    long unreadCount
) {
}
