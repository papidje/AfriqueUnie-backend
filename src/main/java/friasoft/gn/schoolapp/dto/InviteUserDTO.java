package friasoft.gn.schoolapp.dto;

import friasoft.gn.schoolapp.entity.auth.User;

import java.util.List;

public record InviteUserDTO(
    String email,
    String nom,
    User.UserRole role,
    /**
     * Ancien flux : une école + {@link #role()}. Ignoré si {@link #schoolAssignments()} est fourni.
     */
    Long schoolId,
    /**
     * Rattachements multiples (fondateur : enseignant / personnel avec rôle par établissement).
     * Peut aussi contenir une seule entrée pour un directeur ({@link User.UserRole#DIRECTOR}).
     */
    List<SchoolRoleAssignmentDTO> schoolAssignments
) {
}
