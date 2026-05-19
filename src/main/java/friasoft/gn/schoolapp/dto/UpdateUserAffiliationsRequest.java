package friasoft.gn.schoolapp.dto;

import java.util.List;

/** Corps PATCH affiliations : liste souhaitée des rattachements actifs (sync complète). */
public record UpdateUserAffiliationsRequest(List<SchoolRoleAssignmentDTO> assignments) {
}
