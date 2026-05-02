package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.EvaluationDtos.CreateEvaluationRequest;
import friasoft.gn.schoolapp.dto.EvaluationDtos.EvaluationResponse;
import friasoft.gn.schoolapp.dto.EvaluationDtos.GradeSheetResponse;
import friasoft.gn.schoolapp.dto.EvaluationDtos.GradeUpsertRequest;
import friasoft.gn.schoolapp.dto.EvaluationDtos.GradingPeriodSummary;
import friasoft.gn.schoolapp.dto.EvaluationDtos.StudentGradeRowResponse;
import friasoft.gn.schoolapp.dto.TimetableDtos.TimetableEvaluationDto;
import friasoft.gn.schoolapp.entity.school.ClassSubject;
import friasoft.gn.schoolapp.entity.school.Evaluation;
import friasoft.gn.schoolapp.entity.school.Grade;
import friasoft.gn.schoolapp.entity.school.GradingPeriod;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.IClassSubjectRepository;
import friasoft.gn.schoolapp.repository.IEvaluationRepository;
import friasoft.gn.schoolapp.repository.IGradeRepository;
import friasoft.gn.schoolapp.repository.IGradingPeriodRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final IEvaluationRepository evaluationRepository;
    private final IGradeRepository gradeRepository;
    private final IClassSubjectRepository classSubjectRepository;
    private final IGradingPeriodRepository gradingPeriodRepository;
    private final IStudentRepository studentRepository;
    private final ISchoolClassRepository schoolClassRepository;
    private final SchoolService schoolService;

    @Transactional(readOnly = true)
    public List<GradingPeriodSummary> listGradingPeriodsForClass(Long classId) {
        assertClassAccess(classId);
        return gradingPeriodRepository.findBySchoolClass_IdOrderByStartDateAsc(classId).stream()
            .map(
                gp -> new GradingPeriodSummary(
                    gp.getId(),
                    gp.getName(),
                    gp.getStartDate(),
                    gp.getEndDate(),
                    evaluationRepository.countByGradingPeriod_Id(gp.getId()) > 0
                )
            )
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> listForClass(Long classId) {
        assertClassAccess(classId);
        return evaluationRepository.findBySchoolClassIdWithDetails(classId).stream()
            .map(EvaluationService::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TimetableEvaluationDto> listForTimetableWeek(Long classId, LocalDate weekMonday) {
        assertClassAccess(classId);
        if (weekMonday == null) {
            return List.of();
        }
        LocalDateTime rangeStart = weekMonday.atStartOfDay();
        LocalDateTime rangeEnd = weekMonday.plusDays(6).atTime(23, 59, 59);
        return evaluationRepository
            .findForClassOverlappingRange(classId, rangeStart, rangeEnd)
            .stream()
            .map(EvaluationService::toTimetableDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public EvaluationResponse create(Long classId, CreateEvaluationRequest req) {
        assertClassAccess(classId);
        if (req == null) {
            throw new IllegalArgumentException("Corps de requête vide.");
        }
        ClassSubject cs = classSubjectRepository
            .findByIdForManagement(req.classSubjectId())
            .orElseThrow(() -> new IllegalArgumentException("Affectation matière / classe introuvable."));
        if (!cs.getSchoolClass().getId().equals(classId)) {
            throw new IllegalArgumentException("Cette matière n’appartient pas à cette classe.");
        }
        GradingPeriod gp = gradingPeriodRepository
            .findById(req.gradingPeriodId())
            .orElseThrow(() -> new IllegalArgumentException("Période de notation introuvable."));
        if (!gp.getSchoolClass().getId().equals(classId)) {
            throw new IllegalArgumentException("Cette période ne correspond pas à cette classe.");
        }
        if (req.title() == null || req.title().isBlank()) {
            throw new IllegalArgumentException("Le titre est obligatoire.");
        }
        if (req.type() == null) {
            throw new IllegalArgumentException("Le type est obligatoire.");
        }
        if (req.startDate() == null || req.endDate() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }
        if (req.endDate().isBefore(req.startDate())) {
            throw new IllegalArgumentException("La fin doit être postérieure au début.");
        }
        double coeff = req.coefficient() != null && req.coefficient() > 0 ? req.coefficient() : 1.0;
        double maxScore = resolveMaxScore(req.maxScore());

        Evaluation e = new Evaluation();
        e.setTenantId(cs.getTenantId());
        e.setClassSubject(cs);
        e.setGradingPeriod(gp);
        e.setTitle(req.title().trim());
        e.setDescription(req.description() != null ? req.description().trim() : null);
        e.setEvalType(req.type());
        e.setCoefficient(coeff);
        e.setMaxScore(maxScore);
        e.setStartDate(req.startDate());
        e.setEndDate(req.endDate());
        e = evaluationRepository.save(e);
        return toResponse(e);
    }

    @Transactional(readOnly = true)
    public EvaluationResponse getById(Long evaluationId) {
        Evaluation e = evaluationRepository
            .findByIdWithDetails(evaluationId)
            .orElseThrow(() -> new IllegalArgumentException("Évaluation introuvable."));
        assertClassAccess(e.getClassSubject().getSchoolClass().getId());
        return toResponse(e);
    }

    @Transactional(readOnly = true)
    public GradeSheetResponse getGradeSheet(Long evaluationId) {
        Evaluation e = evaluationRepository
            .findByIdWithDetails(evaluationId)
            .orElseThrow(() -> new IllegalArgumentException("Évaluation introuvable."));
        long classId = e.getClassSubject().getSchoolClass().getId();
        assertClassAccess(classId);
        EvaluationResponse evalResponse = toResponse(e);
        List<Grade> existing = gradeRepository.findByEvaluation_IdOrderByStudent_LastNameAscStudent_FirstNameAsc(
            evaluationId
        );
        Map<Long, Grade> byStudent = existing.stream()
            .collect(Collectors.toMap(g -> g.getStudent().getId(), g -> g, (a, b) -> a));
        List<Student> students = studentRepository.findBySchoolClass_IdOrderByLastNameAscFirstNameAsc(classId);
        List<StudentGradeRowResponse> rows = new ArrayList<>();
        for (Student s : students) {
            Grade g = byStudent.get(s.getId());
            rows.add(
                new StudentGradeRowResponse(
                    s.getId(),
                    s.getLastName(),
                    s.getFirstName(),
                    g != null ? g.getId() : null,
                    g != null ? g.getValue() : null,
                    g != null ? g.getComment() : null
                )
            );
        }
        return new GradeSheetResponse(evalResponse, rows);
    }

    @Transactional
    public void saveGrades(Long evaluationId, List<GradeUpsertRequest> requestRows) {
        Evaluation e = evaluationRepository
            .findByIdWithDetails(evaluationId)
            .orElseThrow(() -> new IllegalArgumentException("Évaluation introuvable."));
        long classId = e.getClassSubject().getSchoolClass().getId();
        assertClassAccess(classId);
        if (requestRows == null) {
            return;
        }
        for (GradeUpsertRequest row : requestRows) {
            if (row == null || row.studentId() == null) {
                continue;
            }
            Student s = studentRepository
                .findById(row.studentId())
                .orElseThrow(() -> new IllegalArgumentException("Élève introuvable : " + row.studentId()));
            if (s.getSchoolClass() == null || !Objects.equals(s.getSchoolClass().getId(), classId)) {
                throw new IllegalArgumentException("L’élève n’appartient pas à la classe de l’évaluation.");
            }
            Grade g = gradeRepository
                .findByEvaluation_IdAndStudent_Id(evaluationId, row.studentId())
                .orElseGet(() -> {
                    Grade n = new Grade();
                    n.setTenantId(e.getTenantId());
                    n.setEvaluation(e);
                    n.setStudent(s);
                    return n;
                });
            g.setValue(row.value());
            g.setComment(row.comment() != null && !row.comment().isBlank() ? row.comment().trim() : null);
            gradeRepository.save(g);
        }
    }

    @Transactional
    public void delete(Long evaluationId) {
        Evaluation e = evaluationRepository
            .findByIdWithDetails(evaluationId)
            .orElseThrow(() -> new IllegalArgumentException("Évaluation introuvable."));
        assertClassAccess(e.getClassSubject().getSchoolClass().getId());
        evaluationRepository.delete(e);
    }

    @Transactional
    public EvaluationResponse update(Long evaluationId, CreateEvaluationRequest req) {
        Evaluation e = evaluationRepository
            .findByIdWithDetails(evaluationId)
            .orElseThrow(() -> new IllegalArgumentException("Évaluation introuvable."));
        long classId = e.getClassSubject().getSchoolClass().getId();
        assertClassAccess(classId);
        if (req == null || req.title() == null || req.title().isBlank()) {
            throw new IllegalArgumentException("Le titre est obligatoire.");
        }
        if (req.type() == null || req.startDate() == null || req.endDate() == null) {
            throw new IllegalArgumentException("Type et dates obligatoires.");
        }
        if (req.endDate().isBefore(req.startDate())) {
            throw new IllegalArgumentException("La fin doit être postérieure au début.");
        }
        if (!req.classSubjectId().equals(e.getClassSubject().getId())) {
            throw new IllegalArgumentException("Changer l’affectation matière / classe n’est pas supporté ici.");
        }
        if (!req.gradingPeriodId().equals(e.getGradingPeriod().getId())) {
            GradingPeriod gp = gradingPeriodRepository
                .findById(req.gradingPeriodId())
                .orElseThrow(() -> new IllegalArgumentException("Période introuvable."));
            if (!Objects.equals(gp.getSchoolClass().getId(), classId)) {
                throw new IllegalArgumentException("Période incohérente avec la classe.");
            }
            e.setGradingPeriod(gp);
        }
        e.setTitle(req.title().trim());
        e.setDescription(req.description() != null ? req.description().trim() : null);
        e.setEvalType(req.type());
        e.setCoefficient(req.coefficient() != null && req.coefficient() > 0 ? req.coefficient() : 1.0);
        e.setMaxScore(resolveMaxScore(req.maxScore()));
        e.setStartDate(req.startDate());
        e.setEndDate(req.endDate());
        return toResponse(evaluationRepository.save(e));
    }

    private void assertClassAccess(long classId) {
        SchoolClass sc = schoolClassRepository.findByIdWithYearAndSchool(classId)
            .orElseThrow(() -> new IllegalArgumentException("Classe introuvable."));
        schoolService.assertCurrentUserCanAccessSchool(sc.getYear().getSchool().getId());
    }

    private static EvaluationResponse toResponse(Evaluation e) {
        var cs = e.getClassSubject();
        var sub = cs.getSubject();
        var gp = e.getGradingPeriod();
        return new EvaluationResponse(
            e.getId(),
            cs.getId(),
            gp.getId(),
            gp.getName(),
            e.getTitle(),
            e.getDescription(),
            e.getEvalType(),
            e.getCoefficient(),
            e.getMaxScore() != null ? e.getMaxScore() : 20.0,
            e.getStartDate(),
            e.getEndDate(),
            sub.getCode(),
            sub.getName()
        );
    }

    private static double resolveMaxScore(Double maxScore) {
        if (maxScore == null || maxScore <= 0) {
            return 20.0;
        }
        return maxScore;
    }

    private static TimetableEvaluationDto toTimetableDto(Evaluation e) {
        var cs = e.getClassSubject();
        var sub = cs.getSubject();
        var gp = e.getGradingPeriod();
        return new TimetableEvaluationDto(
            e.getId(),
            e.getTitle(),
            e.getEvalType(),
            cs.getId(),
            sub.getCode(),
            sub.getName(),
            e.getStartDate(),
            e.getEndDate(),
            gp.getId(),
            gp.getName()
        );
    }
}
