package friasoft.gn.schoolapp.dto;

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

    public record TimetableViewDto(Long classId, List<TimetableSlotDto> slots) {
    }

    public record TimetableCellWriteDto(Integer dayOfWeek, Integer slotIndex, Long classSubjectId) {
    }
}
