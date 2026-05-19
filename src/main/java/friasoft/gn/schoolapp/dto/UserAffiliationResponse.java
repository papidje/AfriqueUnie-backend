package friasoft.gn.schoolapp.dto;

/**
 * Affiliation utilisateur ↔ établissement (annuaire admin).
 * {@code active} : {@code user_school_affiliations.is_active} (invitations en attente possibles).
 * {@code invitationPending} : inactive et pas encore visible pour le tenant ({@code show_info_to_tenant = false}).
 * {@code reactivationEligible} : inactive mais visible ({@code show_info_to_tenant = true}), ex. suspension levée par réactivation.
 */
public record UserAffiliationResponse(
    Long schoolId,
    String schoolName,
    String role,
    boolean active,
    boolean invitationPending,
    boolean reactivationEligible
) {}
