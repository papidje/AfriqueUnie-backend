package friasoft.gn.schoolapp.dto;

import java.time.LocalDate;
import java.util.List;

public record UserResponse(
    Long id,
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
    /** Affiliations pour le tenant courant (actives et en attente — voir {@link UserAffiliationResponse#active}). */
    List<UserAffiliationResponse> activeAffiliations,
    /** Libellé masquage annuaire multi-tenant ({@code show_info_to_tenant = false}), ex. « En attente d'acceptation ». */
    String directoryPrivacyStatus
) {

}
