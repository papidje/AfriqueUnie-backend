package friasoft.gn.schoolapp.dto;

import java.time.LocalDateTime;

/**
 * Ligne d’historique de paiement pour la fiche élève (toutes années où un compte existe).
 * Les lignes partageant la même {@code receiptReference} correspondent à un même encaissement.
 */
public record StudentPaymentLedgerRowDTO(
    Long id,
    String paymentType,
    Double amount,
    String currency,
    String paymentMode,
    LocalDateTime paymentDate,
    String schoolYearLabel,
    String receiptReference,
    /** Saisie libre sur l’encaissement (figurait sur le reçu). */
    String recordedBy,
    /** Nom du compte utilisateur ayant enregistré la ligne (historique uniquement). */
    String validatedByUserName,
    /** Libellé du mois pour les lignes SCOLARITE (ex. « Octobre »), null sinon. */
    String tuitionMonthLabel
) {
}
