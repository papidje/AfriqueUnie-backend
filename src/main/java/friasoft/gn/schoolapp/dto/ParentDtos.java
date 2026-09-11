package friasoft.gn.schoolapp.dto;

import java.util.List;

public final class ParentDtos {
    private ParentDtos() {}

    public record ParentWriteRequest(
        String lastName,
        String firstName,
        String phone,
        String email,
        String profession,
        String address
    ) {}

    /** Enfant lié au parent (père et/ou mère). */
    public record ParentChildRow(
        Long id,
        String firstName,
        String lastName,
        String matricule,
        String className,
        String enrollmentStatus,
        /** {@code PERE}, {@code MERE} ou {@code PERE_ET_MERE}. */
        String relation
    ) {}

    public record ParentResponse(
        Long id,
        Long tenantId,
        String lastName,
        String firstName,
        String phone,
        String email,
        String profession,
        String address,
        List<ParentChildRow> children
    ) {
        public ParentResponse {
            children = children == null ? List.of() : List.copyOf(children);
        }
    }
}
