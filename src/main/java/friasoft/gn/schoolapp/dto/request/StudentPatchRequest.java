package friasoft.gn.schoolapp.dto.request;

public record StudentPatchRequest(
    String civility,
    String firstName,
    String lastName,
    /**
     * Date de naissance (yyyy-MM-dd).
     * {@code null} = inchangé ; chaîne vide = effacer.
     */
    String birthDate,
    String birthPlace,
    String nationality,
    String address,
    String communicationPhone,
    String communicationEmail,
    String emergencyContactName,
    String emergencyContactPhone,
    String bloodGroup,
    String allergies,
    String enrollmentStatus,
    String classHistory,
    /** Numéro de carte scolaire (null = inchangé ; chaîne vide = effacer). */
    String cardNumber
) {}
