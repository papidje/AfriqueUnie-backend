package friasoft.gn.schoolapp.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Réponse détaillée pour {@code GET/PATCH /users/...} profil connecté (sans identifiant technique).
 */
public record UserProfileResponse(
    String username,
    String fullname,
    String firstName,
    String lastName,
    LocalDate birthDate,
    String gender,
    String phone,
    String biography,
    String email,
    boolean isActive,
    List<String> roles,
    Instant lastLoginAt,
    List<ProfileSchoolSummary> schools,
    /** Affiliations actives par établissement (même modèle que {@link UserResponse}). */
    List<UserAffiliationResponse> activeAffiliations
) {
}
