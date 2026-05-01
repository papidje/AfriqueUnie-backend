package friasoft.gn.schoolapp.service.document;

import friasoft.gn.schoolapp.dto.GradingDtos.StudentPeriodDashboardResponse;
import friasoft.gn.schoolapp.dto.GradingDtos.StudentPeriodSubjectRow;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.service.GradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bulletin individuel d’un élève (une période) en PDF.
 */
@Service
@RequiredArgsConstructor
public class StudentPeriodBulletinPdfService {

    private static final DecimalFormat SCORE = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.FRANCE));

    private final GradingService gradingService;
    private final IStudentRepository studentRepository;
    private final ISchoolClassRepository schoolClassRepository;
    private final SchoolDocumentBranding schoolDocumentBranding;
    private final PdfService pdfService;

    @Transactional(readOnly = true)
    public byte[] buildPdf(long studentId, long periodId) {
        StudentPeriodDashboardResponse dash = gradingService.buildStudentPeriodDashboard(studentId, periodId);
        Student student = studentRepository
            .findByIdWithParentsAndClass(studentId)
            .orElseThrow(() -> new IllegalStateException("Élève introuvable."));
        SchoolClass schoolClass = schoolClassRepository
            .findByIdWithContextForPdf(dash.classId())
            .orElseThrow(() -> new IllegalStateException("Classe introuvable."));
        School school = schoolClass.getYear().getSchool();
        if (school == null) {
            throw new IllegalStateException("Contexte école introuvable.");
        }

        Map<String, Object> model = new HashMap<>();
        schoolDocumentBranding.putSchoolOnModel(school, model);
        String levelName = schoolClass.getLevel() != null && schoolClass.getLevel().getName() != null
            ? schoolClass.getLevel().getName() : "—";
        String yearLabel = schoolClass.getYear() != null && schoolClass.getYear().getLabel() != null
            ? schoolClass.getYear().getLabel() : "—";
        model.put("className", schoolClass.getName() != null ? schoolClass.getName() : "—");
        model.put("levelName", levelName);
        model.put("yearLabel", yearLabel);
        model.put("periodName", dash.gradingPeriodName());
        model.put("compositionWeight", dash.compositionWeight());
        model.put("studentName", (student.getLastName() != null ? student.getLastName() : "") + " " + (student.getFirstName() != null
            ? student.getFirstName() : ""));
        model.put("matricule", student.getMatricule() != null ? student.getMatricule() : "—");
        model.put("generalAverage", formatScore(dash.generalAverage()));
        model.put("rank", dash.rankInClass() != null ? String.valueOf(dash.rankInClass()) : "—");
        model.put("classSize", dash.classSize());
        model.put("evalCount", dash.evaluatedEvaluationsCount());
        model.put("subjectRows", toSubjectRowViews(dash.subjects()));
        return pdfService.renderFromTemplate("documents/bulletin-eleve-periode", model);
    }

    private static List<SubjectRowView> toSubjectRowViews(List<StudentPeriodSubjectRow> rows) {
        List<SubjectRowView> out = new ArrayList<>(rows.size());
        for (StudentPeriodSubjectRow r : rows) {
            out.add(
                new SubjectRowView(
                    r.subjectCode() != null && !r.subjectCode().isEmpty() ? r.subjectCode() : "—",
                    r.subjectName() != null ? r.subjectName() : "—",
                    r.coefficient(),
                    formatScore(r.continuousAverage()),
                    formatScore(r.compositionAverage()),
                    formatScore(r.periodFinalAverage())
                )
            );
        }
        return out;
    }

    public record SubjectRowView(
        String subjectCode,
        String subjectName,
        double coefficient,
        String continuous,
        String composition,
        String period
    ) {
    }

    private static String formatScore(Double v) {
        if (v == null) {
            return "—";
        }
        return SCORE.format(v);
    }
}
