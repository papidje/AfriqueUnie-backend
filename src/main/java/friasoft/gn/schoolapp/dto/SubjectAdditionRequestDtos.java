package friasoft.gn.schoolapp.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class SubjectAdditionRequestDtos {
    private SubjectAdditionRequestDtos() {}

    public record CreateRequest(
        String subjectName,
        Long classLevelId,
        /** Commentaire initial obligatoire (justification). */
        String comment
    ) {}

    public record AddCommentRequest(String body) {}

    public record AcceptRequest(
        String code,
        String name,
        /** Groupes de cycle ; si vide, le groupe du niveau demandé est utilisé. */
        List<String> levelGroupCodes
    ) {}

    public record RefuseRequest(
        /** Commentaire de clôture optionnel. */
        String comment
    ) {}

    public record CommentResponse(
        Long id,
        Long authorUserId,
        String authorFullName,
        boolean authorIsSuperAdmin,
        String body,
        LocalDateTime createdAt
    ) {}

    public record RequestSummary(
        Long id,
        String subjectName,
        String status,
        Long schoolId,
        String schoolName,
        Long classLevelId,
        String classLevelCode,
        String classLevelName,
        String levelGroupCode,
        String levelGroupName,
        Long requestedByUserId,
        String requestedByFullName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closedAt,
        int commentCount
    ) {}

    public record RequestDetail(
        Long id,
        String subjectName,
        String status,
        Long schoolId,
        String schoolName,
        Long classLevelId,
        String classLevelCode,
        String classLevelName,
        String levelGroupCode,
        String levelGroupName,
        Long requestedByUserId,
        String requestedByFullName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closedAt,
        Long createdSubjectId,
        String createdSubjectCode,
        List<CommentResponse> comments
    ) {}
}
