package friasoft.gn.schoolapp.dto.messaging;

import java.time.Instant;

public record MessagingMessageDto(
    Long id,
    Long conversationId,
    Long senderId,
    String body,
    Instant createdAt,
    boolean mine
) {
}
