package friasoft.gn.schoolapp.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
    long studentsCount,
    BigDecimal monthlyTuitionCollected,
    List<RecentEnrollmentResponse> recentEnrollments
) {
    public record RecentEnrollmentResponse(
        Long id,
        String fullName,
        String className,
        LocalDate enrolledAt
    ) {
    }
}
