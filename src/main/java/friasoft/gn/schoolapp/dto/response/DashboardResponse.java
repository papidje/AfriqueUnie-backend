package friasoft.gn.schoolapp.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
    /** Élèves inscrits dans une classe de l’année scolaire active. */
    long studentsEnrolled,
    /** Somme des capacités des classes de l’année active (places). */
    long totalCapacity,
    /** Nombre de classes (année active). */
    long classesCount,
    /** Paiements rattachés aux comptes de l’année active. */
    BigDecimal schoolYearTuitionCollected,
    /** Remplissage par classe (inscrits vs capacité). */
    List<ClassFillItem> classFill,
    /** Paiements à jour vs en retard par classe (dû à date du jour). */
    List<ClassPaymentStatusItem> classPaymentStatus
) {
    public record ClassFillItem(
        Long classId,
        String className,
        String levelCode,
        long enrolled,
        long capacity
    ) {
    }

    public record ClassPaymentStatusItem(
        Long classId,
        String className,
        String levelCode,
        long upToDateCount,
        long lateCount
    ) {
    }
}
