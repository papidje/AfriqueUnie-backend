package friasoft.gn.schoolapp.dto;

import java.util.List;

public final class ClassSubjectDtos {

    private ClassSubjectDtos() {
    }

    public record ClassSubjectResponse(
        Long id,
        Long classId,
        Long schoolId,
        Long subjectId,
        String subjectCode,
        String subjectName,
        Integer coefficient,
        Long teacherId,
        String teacherFullname
    ) {
    }

    public record ClassPlanningView(
        Long classId,
        String className,
        Long schoolId,
        List<ClassSubjectResponse> subjects
    ) {
    }

    public record TeacherSummaryResponse(Long id, String fullname, String email) {
    }

    public record CreateClassSubjectRequest(Long subjectId, Integer coefficient, Long teacherId) {
    }

    public record UpdateClassSubjectCoefficientRequest(Integer coefficient) {
    }

    public record AssignClassSubjectTeacherRequest(Long teacherId) {
    }
}
