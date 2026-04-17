package friasoft.gn.schoolapp.dto;

import friasoft.gn.schoolapp.entity.auth.User;

public record InviteUserDTO(
    String email,
    String nom,
    User.UserRole role
) {
}
