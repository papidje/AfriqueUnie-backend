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
    Double suppliesExpected,
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

