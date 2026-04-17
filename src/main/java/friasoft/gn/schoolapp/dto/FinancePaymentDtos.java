package friasoft.gn.schoolapp.dto;

import java.util.List;

public final class FinancePaymentDtos {
    private FinancePaymentDtos() {}

    public record CreatePaymentRequest(
        String paymentMode,
        String currency,
        /** Si renseigné (> 0), répartition automatique dans l'ordre : inscription/réinscription, fournitures, mois (Oct→Juin). */
        Double totalDeclaredAmount,
        Boolean payInsReins,
        Double insReinsAmount,
        Boolean paySupplies,
        List<String> months
    ) {}

    public record CreatePaymentResponse(
        Long studentId,
        /** Classe courante de l’élève (retour liste Finance avec onglet). */
        Long schoolClassId,
        Double totalCollected,
        String paymentMode,
        String receiptReference
    ) {}
}

