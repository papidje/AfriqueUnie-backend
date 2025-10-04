package friasoft.gn.schoolapp.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SchoolYearResponse(
    Long id,
    String label,
    LocalDate startDate,
    LocalDate endDate,
    boolean active,
    LocalDateTime createdAt
) {
}
