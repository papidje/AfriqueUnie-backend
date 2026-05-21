package friasoft.gn.schoolapp.dto;

/**
 * Inscription fondateur : création tenant + école + compte {@code ADMIN_ECOLE} (activation par e-mail).
 */
public record RegistrationRequest(
    String username,
    String adminFirstName,
    String adminLastName,
    String email,
    String tenantName,
    String schoolName,
    /** Adresse de l’établissement → colonnes tenant.address et schools.adress. */
    String schoolAddress,
    String tenantLogo,
    /** Téléphone de l’établissement → {@link friasoft.gn.schoolapp.entity.school.School#setContact}. */
    String schoolContact
) {
}
