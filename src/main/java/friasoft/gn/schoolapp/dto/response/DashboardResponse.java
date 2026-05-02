package friasoft.gn.schoolapp.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
    /** Élèves inscrits dans une classe de l’année scolaire active. */
    long studentsEnrolled,
    /** Somme des capacités des classes de l’année active (places). */
    long totalCapacity,
    /** Nombre de classes (année active). */
    long classesCount,
    /** Affectations matière–classe (année active). */
    long taughtSubjectsCount,
    BigDecimal monthlyTuitionCollected,
    /** Paiements rattachés aux comptes de l’année active. */
    BigDecimal schoolYearTuitionCollected,
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
