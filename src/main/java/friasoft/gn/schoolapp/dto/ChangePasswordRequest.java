package friasoft.gn.schoolapp.dto;

/**
 * Changement de mot de passe pour l’utilisateur connecté.
 */
public record ChangePasswordRequest(String currentPassword, String newPassword) {
}
