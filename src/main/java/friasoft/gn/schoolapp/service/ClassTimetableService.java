package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.TimetableDtos.TimetableCellWriteDto;
import friasoft.gn.schoolapp.dto.TimetableDtos.TimetableEvaluationDto;
import friasoft.gn.schoolapp.dto.TimetableDtos.TimetableSlotDto;
import friasoft.gn.schoolapp.dto.TimetableDtos.TimetableViewDto;
import friasoft.gn.schoolapp.entity.school.ClassSubject;
import friasoft.gn.schoolapp.entity.school.ClassTimetableSlot;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.repository.IClassSubjectRepository;
import friasoft.gn.schoolapp.repository.IClassTimetableSlotRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
public class ClassTimetableService {

    private static final int DAY_MIN = 1;
    private static final int DAY_MAX = 5;
    private static final int SLOT_MIN = 0;
    private static final int SLOT_MAX = 7;

    private final IClassTimetableSlotRepository timetableSlotRepository;
    private final ISchoolClassRepository schoolClassRepository;
    private final IClassSubjectRepository classSubjectRepository;
    private final SchoolService schoolService;
    private final EvaluationService evaluationService;

    @Transactional(readOnly = true)
    public TimetableViewDto getTimetable(Long classId) {
        return getTimetable(classId, null, false);
    }

    @Transactional(readOnly = true)
    public TimetableViewDto getTimetable(Long classId, LocalDate weekStart, boolean includeEvaluations) {
        SchoolClass clazz = loadClassForAccess(classId);
        assertAccess(clazz);
        List<TimetableSlotDto> slots = timetableSlotRepository.findBySchoolClassIdWithSubject(classId).stream()
            .map(this::toDto)
            .toList();
        List<TimetableEvaluationDto> evals = List.of();
        if (includeEvaluations && weekStart != null) {
            evals = evaluationService.listForTimetableWeek(classId, weekStart);
        }
        return new TimetableViewDto(classId, slots, evals);
    }

    @Transactional
    public TimetableViewDto setCell(Long classId, TimetableCellWriteDto body) {
        SchoolClass clazz = loadClassForAccess(classId);
        assertAccess(clazz);
        if (body.dayOfWeek() == null || body.slotIndex() == null) {
            throw new IllegalArgumentException("dayOfWeek et slotIndex sont obligatoires.");
        }
        int day = body.dayOfWeek();
        int slot = body.slotIndex();
        if (day < DAY_MIN || day > DAY_MAX || slot < SLOT_MIN || slot > SLOT_MAX) {
            throw new IllegalArgumentException("Créneau invalide (jour 1–5, période 0–7).");
        }
        if (body.classSubjectId() == null) {
            timetableSlotRepository.deleteBySchoolClass_IdAndDayOfWeekAndSlotIndex(classId, day, slot);
            return getTimetable(classId);
        }
        ClassSubject cs = classSubjectRepository.findById(body.classSubjectId())
            .orElseThrow(() -> new IllegalArgumentException("Liaison classe-matière introuvable."));
        if (!cs.getSchoolClass().getId().equals(classId)) {
            throw new IllegalArgumentException("Cette matière n’appartient pas à cette classe.");
        }
        ClassTimetableSlot entity = timetableSlotRepository
            .findBySchoolClass_IdAndDayOfWeekAndSlotIndex(classId, day, slot)
            .orElseGet(ClassTimetableSlot::new);
        entity.setSchoolClass(clazz);
        entity.setTenantId(clazz.getTenantId());
        entity.setDayOfWeek(day);
        entity.setSlotIndex(slot);
        entity.setClassSubject(cs);
        timetableSlotRepository.save(entity);
        return getTimetable(classId, null, false);
    }

    private SchoolClass loadClassForAccess(Long classId) {
        return schoolClassRepository.findByIdWithYearAndSchool(classId)
            .orElseThrow(() -> new IllegalArgumentException("Classe introuvable."));
    }

    private void assertAccess(SchoolClass clazz) {
        schoolService.assertCurrentUserCanAccessSchool(clazz.getYear().getSchool().getId());
    }

    private TimetableSlotDto toDto(ClassTimetableSlot t) {
        var cs = t.getClassSubject();
        var s = cs.getSubject();
        var teacher = cs.getTeacher();
        return new TimetableSlotDto(
            t.getId(),
            t.getDayOfWeek(),
            t.getSlotIndex(),
            cs.getId(),
            s.getCode(),
            s.getName(),
            teacher != null ? teacher.getFullname() : null
        );
    }
}
