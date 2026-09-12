package friasoft.gn.schoolapp.dto.messaging;

import java.time.Instant;

public record MessagingUserSummaryDto(
    Long id,
    String fullname,
    String email,
    String roleLabel
) {
}
