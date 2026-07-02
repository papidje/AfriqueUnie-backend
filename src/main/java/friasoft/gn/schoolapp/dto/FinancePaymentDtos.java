package friasoft.gn.schoolapp.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class FinancePaymentDtos {
    private FinancePaymentDtos() {}

    public record ReceiptLine(String paymentType, Double amount, String tuitionMonthLabel) {}

    public record CreatePaymentRequest(
        String paymentMode,
        String currency,
        /** Si renseigné (> 0), répartition automatique dans l'ordre : inscription/réinscription, fournitures, mois (Oct→Juin). */
        Double totalDeclaredAmount,
        Boolean payInsReins,
        Double insReinsAmount,
        Boolean paySupplies,
        List<String> months,
        /** Nom de la personne ayant enregistré l’encaissement (obligatoire). */
        String recordedBy
    ) {}

    public record CreatePaymentResponse(
        Long studentId,
        /** Classe courante de l’élève (retour liste Finance avec onglet). */
        Long schoolClassId,
        Double totalCollected,
        String paymentMode,
        String receiptReference,
        String recordedBy,
        List<ReceiptLine> lines
    ) {}

    /** Données pour réimpression / duplicata de reçu (même référence). */
    public record PaymentReceiptView(
        String studentName,
        String matricule,
        String schoolYearLabel,
        String receiptReference,
        String recordedBy,
        String paymentMode,
        String currency,
        LocalDateTime paymentDate,
        List<ReceiptLine> lines,
        Double totalCollected,
        /** Reliquat dû après cet encaissement (état comptable post-paiement). */
        Double balanceRemaining,
        boolean duplicate
    ) {}
}
