package friasoft.gn.schoolapp.dto;

/**
 * Représentant légal unique à l’inscription.
 * {@code relation} : {@code PERE}, {@code MERE} ou {@code TUTEUR}.
 */
public record LegalGuardianRegistrationDTO(
    String relation,
    String civility,
    String lastName,
    String firstName,
    String phone,
    String email,
    String profession,
    String address
) {}
