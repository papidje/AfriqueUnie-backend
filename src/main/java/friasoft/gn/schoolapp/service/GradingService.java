package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.GradingDtos.ClassSubjectColumn;
import friasoft.gn.schoolapp.dto.GradingDtos.PeriodAverageBreakdown;
import friasoft.gn.schoolapp.dto.GradingDtos.PeriodNotesGridResponse;
import friasoft.gn.schoolapp.dto.GradingDtos.PeriodNotesGridRow;
import friasoft.gn.schoolapp.dto.GradingDtos.StudentPeriodBuildData;
import friasoft.gn.schoolapp.dto.GradingDtos.StudentPeriodDashboardResponse;
import friasoft.gn.schoolapp.dto.GradingDtos.StudentPeriodSubjectRow;
import friasoft.gn.schoolapp.entity.school.ClassSubject;
import friasoft.gn.schoolapp.entity.school.Evaluation;
import friasoft.gn.schoolapp.entity.school.EvaluationType;
import friasoft.gn.schoolapp.entity.school.Grade;
import friasoft.gn.schoolapp.entity.school.GradingPeriod;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.IGradeRepository;
import friasoft.gn.schoolapp.repository.IGradingPeriodRepository;
import friasoft.gn.schoolapp.repository.IClassSubjectRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Calcul des moyennes périodiques : contrôle continu (interros / devoirs / quiz) pondéré, puis
 * intégration de la moyenne de composition pour la moyenne finale de période.
 * Les lectures ciblées (fiche élève, grille) préfèrent {@link GradingSnapshotReadService} quand un batch a peuplé les snapshots.
 */
@Service
public class GradingService {

    private static final Set<EvaluationType> CONTINUOUS_TYPES =
        EnumSet.of(EvaluationType.INTERROGATION, EvaluationType.DEVOIR, EvaluationType.QUIZ);

    private final IGradeRepository gradeRepository;
    private final IGradingPeriodRepository gradingPeriodRepository;
    private final IClassSubjectRepository classSubjectRepository;
    private final IStudentRepository studentRepository;
    private final SchoolService schoolService;
    private final GradingSnapshotReadService snapshotReadService;

    private final double compositionWeightInPeriod;

    public GradingService(
        IGradeRepository gradeRepository,
        IGradingPeriodRepository gradingPeriodRepository,
        IClassSubjectRepository classSubjectRepository,
        IStudentRepository studentRepository,
        SchoolService schoolService,
        GradingSnapshotReadService snapshotReadService,
        @Value("${application.grading.composition-weight-in-period:0.5}") double compositionWeightInPeriod
    ) {
        this.gradeRepository = gradeRepository;
        this.gradingPeriodRepository = gradingPeriodRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.studentRepository = studentRepository;
        this.schoolService = schoolService;
        this.snapshotReadService = snapshotReadService;
        this.compositionWeightInPeriod = clamp01(compositionWeightInPeriod);
    }

    @Transactional(readOnly = true)
    public PeriodAverageBreakdown computePeriodAverageForStudent(long studentId, long gradingPeriodId) {
        Student student = studentRepository
            .findByIdWithParentsAndClass(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Élève introuvable."));
        if (student.getSchoolClass() == null) {
            throw new IllegalArgumentException("L’élève n’est affecté à aucune classe.");
        }
        long schoolId = student.getSchoolClass().getYear().getSchool().getId();
        schoolService.assertCurrentUserCanAccessSchool(schoolId);

        GradingPeriod period = gradingPeriodRepository
            .findByIdWithClassAndSchool(gradingPeriodId)
            .orElseThrow(() -> new IllegalArgumentException("Période de notation introuvable."));

        if (!Objects.equals(period.getSchoolClass().getId(), student.getSchoolClass().getId())) {
            throw new IllegalArgumentException("Cette période de notation ne concerne pas la classe de l’élève.");
        }

        List<Grade> grades = gradeRepository.findByStudentIdAndGradingPeriodIdWithEvaluations(
            studentId,
            gradingPeriodId
        );

        Double finalAvg = periodFinalAverageForGrades(grades, compositionWeightInPeriod);
        WeightedBlock continuous = computeWeightedBlock(grades, CONTINUOUS_TYPES::contains);
        WeightedBlock composition = computeWeightedBlock(grades, t -> t == EvaluationType.COMPOSITION);
        Double continuousAvg = continuous.average;
        Double compositionAvg = composition.average;

        return new PeriodAverageBreakdown(
            studentId,
            gradingPeriodId,
            period.getName(),
            continuousAvg,
            compositionAvg,
            finalAvg,
            compositionWeightInPeriod,
            continuous.count,
            composition.count
        );
    }

    /**
     * Grille : moyenne périodique par matière puis moyenne générale. Préfère le snapshot s’il est complet
     * pour la classe et la période ; sinon calcul live.
     */
    @Transactional(readOnly = true)
    public PeriodNotesGridResponse buildPeriodNotesGrid(long classId, long gradingPeriodId) {
        GradingPeriod period = gradingPeriodRepository
            .findByIdWithClassAndSchool(gradingPeriodId)
            .orElseThrow(() -> new IllegalArgumentException("Période de notation introuvable."));

        if (!Objects.equals(period.getSchoolClass().getId(), classId)) {
            throw new IllegalArgumentException("Cette période de notation ne correspond pas à cette classe.");
        }
        long schoolId = period.getSchoolClass().getYear().getSchool().getId();
        schoolService.assertCurrentUserCanAccessSchool(schoolId);

        return snapshotReadService
            .toPeriodNotesGridIfComplete(classId, gradingPeriodId)
            .orElseGet(() -> buildGridCoreLive(classId, gradingPeriodId, period));
    }

    /**
     * Même moteur que la grille, sans droits utilisateur (job batch, tenant posé en amont).
     */
    @Transactional(readOnly = true)
    public PeriodNotesGridResponse buildPeriodNotesGridLiveOnly(long classId, long gradingPeriodId) {
        GradingPeriod period = gradingPeriodRepository
            .findByIdWithClassAndSchool(gradingPeriodId)
            .orElseThrow(() -> new IllegalArgumentException("Période de notation introuvable."));

        if (!Objects.equals(period.getSchoolClass().getId(), classId)) {
            throw new IllegalArgumentException("Cette période de notation ne correspond pas à cette classe.");
        }
        return buildGridCoreLive(classId, gradingPeriodId, period);
    }

    private PeriodNotesGridResponse buildGridCoreLive(long classId, long gradingPeriodId, GradingPeriod period) {
        List<ClassSubject> classSubjects = classSubjectRepository.findBySchoolClassIdWithSubject(classId);
        List<ClassSubjectColumn> columns = new ArrayList<>(classSubjects.size());
        for (ClassSubject cs : classSubjects) {
            double coeff = classSubjectCoeff(cs);
            var subj = cs.getSubject();
            columns.add(
                new ClassSubjectColumn(
                    cs.getId(),
                    subj.getCode() != null ? subj.getCode() : "",
                    subj.getName() != null ? subj.getName() : "",
                    coeff
                )
            );
        }

        List<Student> students = studentRepository.findBySchoolClass_IdOrderByLastNameAscFirstNameAsc(classId);
        List<Grade> allGrades = gradeRepository.findAllForClassAndPeriod(classId, gradingPeriodId);

        Map<Long, Map<Long, List<Grade>>> byStudentAndSubject = new HashMap<>();
        for (Grade g : allGrades) {
            if (g.getStudent() == null || g.getEvaluation() == null || g.getEvaluation().getClassSubject() == null) {
                continue;
            }
            long sid = g.getStudent().getId();
            long csid = g.getEvaluation().getClassSubject().getId();
            byStudentAndSubject
                .computeIfAbsent(sid, k -> new HashMap<>())
                .computeIfAbsent(csid, k -> new ArrayList<>())
                .add(g);
        }

        double w = compositionWeightInPeriod;
        List<PeriodNotesGridRow> rows = new ArrayList<>(students.size());
        for (Student s : students) {
            Map<Long, List<Grade>> perSubject = byStudentAndSubject.getOrDefault(s.getId(), Map.of());
            List<Double> averages = new ArrayList<>(classSubjects.size());
            for (ClassSubject cs : classSubjects) {
                List<Grade> subjectGrades = perSubject.getOrDefault(cs.getId(), List.of());
                averages.add(periodFinalAverageForGrades(subjectGrades, w));
            }
            rows.add(
                new PeriodNotesGridRow(
                    s.getId(),
                    s.getLastName() != null ? s.getLastName() : "",
                    s.getFirstName() != null ? s.getFirstName() : "",
                    averages,
                    generalAverageFromColumns(averages, classSubjects)
                )
            );
        }

        return new PeriodNotesGridResponse(
            classId,
            gradingPeriodId,
            period.getName(),
            w,
            columns,
            rows,
            null,
            false
        );
    }

    /**
     * Données de détail (par matière) + moyenne générale + comptage d’évaluations, pour le batch ou relecture.
     */
    @Transactional(readOnly = true)
    public StudentPeriodBuildData buildStudentPeriodBuildData(long studentId, long classId, long gradingPeriodId) {
        List<ClassSubject> classSubjects = classSubjectRepository.findBySchoolClassIdWithSubject(classId);
        List<Grade> grades = gradeRepository.findByStudentIdAndGradingPeriodIdWithEvaluations(
            studentId,
            gradingPeriodId
        );

        Map<Long, List<Grade>> bySubject = new HashMap<>();
        for (Grade g : grades) {
            if (g.getEvaluation() == null || g.getEvaluation().getClassSubject() == null) {
                continue;
            }
            long csid = g.getEvaluation().getClassSubject().getId();
            bySubject.computeIfAbsent(csid, k -> new ArrayList<>()).add(g);
        }

        double w = compositionWeightInPeriod;
        List<StudentPeriodSubjectRow> subjectRows = new ArrayList<>(classSubjects.size());
        List<Double> periodAvgsForGeneral = new ArrayList<>(classSubjects.size());
        for (ClassSubject cs : classSubjects) {
            List<Grade> subjectGrades = bySubject.getOrDefault(cs.getId(), List.of());
            WeightedBlock c = computeWeightedBlock(subjectGrades, CONTINUOUS_TYPES::contains);
            WeightedBlock comp = computeWeightedBlock(subjectGrades, t -> t == EvaluationType.COMPOSITION);
            Double periodFinal = periodFinalAverageForGrades(subjectGrades, w);
            var subj = cs.getSubject();
            subjectRows.add(
                new StudentPeriodSubjectRow(
                    cs.getId(),
                    subj.getCode() != null ? subj.getCode() : "",
                    subj.getName() != null ? subj.getName() : "",
                    classSubjectCoeff(cs),
                    c.average(),
                    comp.average(),
                    periodFinal
                )
            );
            periodAvgsForGeneral.add(periodFinal);
        }

        Double general = generalAverageFromColumns(periodAvgsForGeneral, classSubjects);
        Set<Long> evalIds = new HashSet<>();
        for (Grade g : grades) {
            if (g.getValue() != null && g.getEvaluation() != null) {
                evalIds.add(g.getEvaluation().getId());
            }
        }
        return new StudentPeriodBuildData(subjectRows, general, evalIds.size());
    }

    /**
     * Préfère le snapshot s’il existe ; sinon recalcul live (hors lot).
     */
    @Transactional(readOnly = true)
    public StudentPeriodDashboardResponse buildStudentPeriodDashboard(long studentId, long gradingPeriodId) {
        Student student = studentRepository
            .findByIdWithParentsAndClass(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Élève introuvable."));
        if (student.getSchoolClass() == null) {
            throw new IllegalArgumentException("L’élève n’est affecté à aucune classe.");
        }
        long classId = student.getSchoolClass().getId();
        long schoolId = student.getSchoolClass().getYear().getSchool().getId();
        schoolService.assertCurrentUserCanAccessSchool(schoolId);

        GradingPeriod period = gradingPeriodRepository
            .findByIdWithClassAndSchool(gradingPeriodId)
            .orElseThrow(() -> new IllegalArgumentException("Période de notation introuvable."));

        if (!Objects.equals(period.getSchoolClass().getId(), classId)) {
            throw new IllegalArgumentException("Cette période de notation ne concerne pas la classe de l’élève.");
        }

        return snapshotReadService
            .toStudentPeriodDashboardIfPresent(studentId, gradingPeriodId)
            .orElseGet(() -> buildStudentPeriodDashboardLive(studentId, gradingPeriodId, classId, period));
    }

    private StudentPeriodDashboardResponse buildStudentPeriodDashboardLive(
        long studentId,
        long gradingPeriodId,
        long classId,
        GradingPeriod period
    ) {
        StudentPeriodBuildData build = buildStudentPeriodBuildData(studentId, classId, gradingPeriodId);
        PeriodNotesGridResponse grid = buildGridCoreLive(classId, gradingPeriodId, period);
        int classSize = grid.rows().size();
        Integer rank = null;
        for (var r : grid.rows()) {
            if (r.studentId() == studentId && r.generalAverage() != null) {
                double myG = r.generalAverage();
                int better = 0;
                for (var other : grid.rows()) {
                    if (other.generalAverage() != null && other.generalAverage() > myG + 1e-6) {
                        better++;
                    }
                }
                rank = better + 1;
                break;
            }
        }

        return new StudentPeriodDashboardResponse(
            studentId,
            gradingPeriodId,
            period.getName(),
            classId,
            compositionWeightInPeriod,
            build.generalAverage(),
            rank,
            classSize,
            build.evaluatedEvaluationsCount(),
            build.subjects(),
            null,
            false
        );
    }

    private static double classSubjectCoeff(ClassSubject cs) {
        if (cs.getCoefficient() != null && cs.getCoefficient() > 0) {
            return cs.getCoefficient();
        }
        return 1.0;
    }

    private static Double periodFinalAverageForGrades(List<Grade> grades, double compositionWeight) {
        WeightedBlock continuous = computeWeightedBlock(grades, CONTINUOUS_TYPES::contains);
        WeightedBlock composition = computeWeightedBlock(grades, t -> t == EvaluationType.COMPOSITION);
        return mergePeriodAverages(continuous.average, composition.average, compositionWeight);
    }

    private static Double generalAverageFromColumns(List<Double> perSubjectAverages, List<ClassSubject> classSubjects) {
        if (perSubjectAverages.size() != classSubjects.size()) {
            return null;
        }
        double sum = 0;
        double sumCoef = 0;
        for (int i = 0; i < classSubjects.size(); i++) {
            Double m = perSubjectAverages.get(i);
            if (m == null) {
                continue;
            }
            double c = classSubjectCoeff(classSubjects.get(i));
            sum += m * c;
            sumCoef += c;
        }
        if (sumCoef <= 0) {
            return null;
        }
        return round2(sum / sumCoef);
    }

    private static double clamp01(double w) {
        if (w < 0) {
            return 0;
        }
        if (w > 1) {
            return 1;
        }
        return w;
    }

    private static Double mergePeriodAverages(Double continuous, Double composition, double w) {
        if (continuous == null && composition == null) {
            return null;
        }
        if (continuous == null) {
            return round2(composition);
        }
        if (composition == null) {
            return round2(continuous);
        }
        double merged = (1.0 - w) * continuous + w * composition;
        return round2(merged);
    }

    private static WeightedBlock computeWeightedBlock(
        List<Grade> grades,
        java.util.function.Predicate<EvaluationType> includeType
    ) {
        double sumWeighted = 0;
        double sumCoeff = 0;
        int count = 0;
        for (Grade g : grades) {
            Evaluation e = g.getEvaluation();
            if (e == null || !includeType.test(e.getEvalType())) {
                continue;
            }
            if (g.getValue() == null) {
                continue;
            }
            double max = effectiveMaxScore(e);
            if (max <= 0) {
                continue;
            }
            double coeff = effectiveCoeff(e);
            if (coeff <= 0) {
                continue;
            }
            double on20 = (g.getValue() / max) * 20.0;
            sumWeighted += on20 * coeff;
            sumCoeff += coeff;
            count++;
        }
        if (sumCoeff <= 0) {
            return new WeightedBlock(null, 0);
        }
        return new WeightedBlock(round2(sumWeighted / sumCoeff), count);
    }

    private static double effectiveMaxScore(Evaluation e) {
        if (e.getMaxScore() != null && e.getMaxScore() > 0) {
            return e.getMaxScore();
        }
        return 20.0;
    }

    private static double effectiveCoeff(Evaluation e) {
        if (e.getCoefficient() != null && e.getCoefficient() > 0) {
            return e.getCoefficient();
        }
        return 1.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private record WeightedBlock(Double average, int count) {
    }
}
