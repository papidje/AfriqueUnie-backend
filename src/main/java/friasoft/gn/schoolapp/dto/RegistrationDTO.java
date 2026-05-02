package friasoft.gn.schoolapp.dto;

public record RegistrationDTO(
    StudentRegistrationDTO student,
    ParentRegistrationDTO father,
    ParentRegistrationDTO mother,
    Long classId,
    /** Facultatif : encaissement désormais sur l’écran finance. Absent ou 0 → aucun paiement à l’inscription. */
    Double amountPaid,
    String currency,
    /** {@link friasoft.gn.schoolapp.entity.school.Payment.PaymentMode} (ex. ESPECES) ; utilisé seulement si {@code amountPaid} &gt; 0. */
    String paymentMode
) {
}

