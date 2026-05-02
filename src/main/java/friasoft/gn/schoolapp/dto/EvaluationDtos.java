package friasoft.gn.schoolapp.dto;

import friasoft.gn.schoolapp.entity.school.EvaluationType;
import friasoft.gn.schoolapp.entity.school.PeriodType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class EvaluationDtos {

    private EvaluationDtos() {
    }

    public record CreateEvaluationRequest(
        Long classSubjectId,
        Long gradingPeriodId,
        String title,
        String description,
        EvaluationType type,
        Double coefficient,
        Double maxScore,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
    }

    public record EvaluationResponse(
        Long id,
        Long classSubjectId,
        Long gradingPeriodId,
        String gradingPeriodName,
        String title,
        String description,
        EvaluationType type,
        Double coefficient,
        Double maxScore,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String subjectCode,
        String subjectName
    ) {
    }

    public record StudentGradeRowResponse(
        Long studentId,
        String lastName,
        String firstName,
        Long gradeId,
        Double value,
        String comment
    ) {
    }

    public record GradeSheetResponse(
        EvaluationResponse evaluation,
        List<StudentGradeRowResponse> rows
    ) {
    }

    public record GradeUpsertRequest(Long studentId, Double value, String comment) {
    }

    public record GradingPeriodSummary(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean locked
    ) {
    }

    /**
     * Mise à jour des dates (et libellé optionnel) d’une ou plusieurs périodes — période verrouillée si évaluations.
     */
    public record GradingPeriodDateItem(Long id, LocalDate startDate, LocalDate endDate, String name) {
    }

    public record UpdateGradingPeriodsScheduleRequest(List<GradingPeriodDateItem> periods) {
    }

    public record UpdateClassPeriodTypeRequest(PeriodType periodType) {
    }
}
