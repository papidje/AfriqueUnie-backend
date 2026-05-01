package friasoft.gn.schoolapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import friasoft.gn.schoolapp.dto.GradingDtos.ClassSubjectColumn;
import friasoft.gn.schoolapp.dto.GradingDtos.PeriodNotesGridResponse;
import friasoft.gn.schoolapp.dto.GradingDtos.PeriodNotesGridRow;
import friasoft.gn.schoolapp.dto.GradingDtos.StudentPeriodDashboardResponse;
import friasoft.gn.schoolapp.dto.GradingDtos.StudentPeriodSubjectRow;
import friasoft.gn.schoolapp.entity.school.ClassSubject;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.entity.school.StudentGradingSnapshot;
import friasoft.gn.schoolapp.repository.IClassSubjectRepository;
import friasoft.gn.schoolapp.repository.IStudentGradingSnapshotRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;

/**
 * Lecture des snapshots de moyennes (ligne complète de classe = même lot).
 */
@Service
@RequiredArgsConstructor
public class GradingSnapshotReadService {

    private final IStudentGradingSnapshotRepository snapshotRepository;
    private final IStudentRepository studentRepository;
    private final IClassSubjectRepository classSubjectRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<PeriodNotesGridResponse> toPeriodNotesGridIfComplete(long classId, long gradingPeriodId) {
        long nStudents = studentRepository.countBySchoolClass_Id(classId);
        if (nStudents == 0) {
            return Optional.empty();
        }
        long nSnap = snapshotRepository.countBySchoolClass_IdAndGradingPeriod_Id(classId, gradingPeriodId);
        if (nSnap != nStudents) {
            return Optional.empty();
        }
        List<StudentGradingSnapshot> snaps = snapshotRepository.findByClassAndPeriodWithStudents(
            classId,
            gradingPeriodId
        );
        if (snaps.size() != nStudents) {
            return Optional.empty();
        }
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
        StudentGradingSnapshot first = snaps.get(0);
        double w = first.getCompositionWeight();
        String periodName = first.getGradingPeriodName();
        Instant asOf = snaps.stream()
            .map(StudentGradingSnapshot::getLastUpdatedAt)
            .max(Comparator.naturalOrder())
            .orElse(null);
        List<PeriodNotesGridRow> rows = new ArrayList<>(snaps.size());
        for (StudentGradingSnapshot s : snaps) {
            List<StudentPeriodSubjectRow> subjectRows = parseSubjects(s.getSubjectAveragesJson());
            List<Double> averages = new ArrayList<>(classSubjects.size());
            for (ClassSubject cs : classSubjects) {
                Double m = null;
                for (StudentPeriodSubjectRow r : subjectRows) {
                    if (r.classSubjectId() == cs.getId()) {
                        m = r.periodFinalAverage();
                        break;
                    }
                }
                averages.add(m);
            }
            Student st = s.getStudent();
            rows.add(
                new PeriodNotesGridRow(
                    st.getId(),
                    st.getLastName() != null ? st.getLastName() : "",
                    st.getFirstName() != null ? st.getFirstName() : "",
                    averages,
                    s.getPeriodGeneralAverage()
                )
            );
        }
        return Optional.of(
            new PeriodNotesGridResponse(
                classId,
                gradingPeriodId,
                periodName,
                w,
                columns,
                rows,
                asOf,
                true
            )
        );
    }

    @Transactional(readOnly = true)
    public Optional<StudentPeriodDashboardResponse> toStudentPeriodDashboardIfPresent(
        long studentId,
        long gradingPeriodId
    ) {
        return snapshotRepository
            .findByStudentAndPeriodWithStudentAndClass(studentId, gradingPeriodId)
            .map(this::toDashboard);
    }

    private StudentPeriodDashboardResponse toDashboard(StudentGradingSnapshot e) {
        List<StudentPeriodSubjectRow> subjects = parseSubjects(e.getSubjectAveragesJson());
        long classId = e.getSchoolClass().getId();
        int classSize = (int) studentRepository.countBySchoolClass_Id(classId);
        return new StudentPeriodDashboardResponse(
            e.getStudent().getId(),
            e.getGradingPeriod().getId(),
            e.getGradingPeriodName(),
            classId,
            e.getCompositionWeight(),
            e.getPeriodGeneralAverage(),
            e.getRankInClass(),
            classSize,
            e.getTotalEvaluations(),
            subjects,
            e.getLastUpdatedAt(),
            true
        );
    }

    private static double classSubjectCoeff(ClassSubject cs) {
        if (cs.getCoefficient() != null && cs.getCoefficient() > 0) {
            return cs.getCoefficient();
        }
        return 1.0;
    }

    private List<StudentPeriodSubjectRow> parseSubjects(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<StudentPeriodSubjectRow>>() { });
        } catch (Exception ex) {
            throw new IllegalStateException("subject_averages JSON illisible", ex);
        }
    }
}
