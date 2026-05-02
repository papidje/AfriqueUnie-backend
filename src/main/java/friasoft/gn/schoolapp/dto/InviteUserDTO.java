package friasoft.gn.schoolapp.dto;

import friasoft.gn.schoolapp.entity.auth.User;

public record InviteUserDTO(
    String email,
    String nom,
    User.UserRole role,
    /**
     * Établissement : obligatoire pour {@link User.UserRole#DIRECTOR}, {@link User.UserRole#STAFF},
     * {@link User.UserRole#TEACHER} ; absent pour {@link User.UserRole#ADMIN_ECOLE} (périmètre tout le tenant).
     * Persisté en {@code users.school_id} pour les rôles rattachés à un établissement.
     */
    Long schoolId
) {
}
