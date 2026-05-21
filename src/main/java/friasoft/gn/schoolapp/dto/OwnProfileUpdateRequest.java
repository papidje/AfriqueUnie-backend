package friasoft.gn.schoolapp.dto;

import java.time.LocalDate;

/**
 * Mise à jour du profil pour l’utilisateur connecté ({@code PATCH /users/profile}).
 */
public record OwnProfileUpdateRequest(
    /** Rétrocompatibilité : utilisé si {@code lastName} absent. */
    String fullname,
    String firstName,
    String lastName,
    LocalDate birthDate,
    String gender,
    String phone,
    String biography
) {
}
