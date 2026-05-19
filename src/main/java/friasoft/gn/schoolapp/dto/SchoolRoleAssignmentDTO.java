package friasoft.gn.schoolapp.dto;

import friasoft.gn.schoolapp.entity.auth.User;

/** Rattachement demandé à l’invitation (une ligne par établissement). */
public record SchoolRoleAssignmentDTO(Long schoolId, User.UserRole role) {}
