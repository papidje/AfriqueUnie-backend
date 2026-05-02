package friasoft.gn.schoolapp.dto.response;

import java.time.LocalDateTime;

public record SchoolClassResponse(
    Long id,
    String name,
    Long yearId,
    Long levelId,
    LocalDateTime createdAt
) {
}
