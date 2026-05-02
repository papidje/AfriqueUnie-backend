package friasoft.gn.schoolapp.dto;

import java.util.List;

public record StudentPaymentInfoDTO(
    Long studentId,
    /** Classe courante de l’élève (année active), pour navigation après encaissement. */
    Long schoolClassId,
    String studentName,
    String matricule,
    String insReinsType,
    Double insReinsExpected,
    Double insReinsPaid,
    Double insReinsRemaining,
    boolean suppliesPaid,
    /** Montant attendu (0 si la colonne fournitures est désactivée dans la structure de frais). */
    Double suppliesExpected,
    boolean suppliesColumnEnabled,
    List<MonthlyTuitionStatusDTO> monthlyTuition
) {
    public record MonthlyTuitionStatusDTO(
        String monthCode,
        String monthLabel,
        Double dueAmount,
        Double paidAmount,
        String status
    ) {}
}

