package friasoft.gn.schoolapp.dto;

import friasoft.gn.schoolapp.entity.school.EvaluationType;

import java.time.LocalDateTime;
import java.util.List;

public final class TimetableDtos {

    private TimetableDtos() {
    }

    public record TimetableSlotDto(
        Long id,
        int dayOfWeek,
        int slotIndex,
        Long classSubjectId,
        String subjectCode,
        String subjectName,
        String teacherFullname
    ) {
    }

    /** Évaluation dont l’intervalle chevauche la semaine demandée (optionnel sur l’API emploi du temps). */
    public record TimetableEvaluationDto(
        Long id,
        String title,
        EvaluationType type,
        Long classSubjectId,
        String subjectCode,
        String subjectName,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long gradingPeriodId,
        String gradingPeriodName
    ) {
    }

    public record TimetableViewDto(Long classId, List<TimetableSlotDto> slots, List<TimetableEvaluationDto> evaluations) {
        public TimetableViewDto(Long classId, List<TimetableSlotDto> slots) {
            this(classId, slots, List.of());
        }
    }

    public record TimetableCellWriteDto(Integer dayOfWeek, Integer slotIndex, Long classSubjectId) {
    }
}
