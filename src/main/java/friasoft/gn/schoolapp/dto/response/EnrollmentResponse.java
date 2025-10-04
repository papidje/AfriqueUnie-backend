package friasoft.gn.schoolapp.dto.response;

import java.time.LocalDate;

public record EnrollmentResponse(
    Long id,
    Long studentId,
    Long classId,
    LocalDate enrolledOn,
    LocalDate leftOn,
    String note
) {
}
