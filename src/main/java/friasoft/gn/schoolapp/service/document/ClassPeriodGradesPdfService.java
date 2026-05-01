package friasoft.gn.schoolapp.service.document;

import friasoft.gn.schoolapp.dto.GradingDtos.PeriodNotesGridResponse;
import friasoft.gn.schoolapp.dto.GradingDtos.PeriodNotesGridRow;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
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
 * Relevé de notes de classe (moyennes par période) en PDF.
 */
@Service
@RequiredArgsConstructor
public class ClassPeriodGradesPdfService {

    private static final DecimalFormat SCORE = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.FRANCE));

    private final GradingService gradingService;
    private final ISchoolClassRepository schoolClassRepository;
    private final SchoolDocumentBranding schoolDocumentBranding;
    private final PdfService pdfService;

    @Transactional(readOnly = true)
    public byte[] buildPdf(long classId, long periodId) {
        PeriodNotesGridResponse grid = gradingService.buildPeriodNotesGrid(classId, periodId);
        SchoolClass schoolClass = schoolClassRepository
            .findByIdWithContextForPdf(classId)
            .orElseThrow(() -> new IllegalArgumentException("Classe introuvable."));
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
        model.put("periodName", grid.gradingPeriodName());
        model.put("compositionWeight", grid.compositionWeight());
        model.put("columns", grid.columns());
        model.put("rowViews", toRowViews(grid));
        return pdfService.renderFromTemplate("documents/releve-notes-classe", model);
    }

    private static List<PeriodNotesRowView> toRowViews(PeriodNotesGridResponse grid) {
        List<PeriodNotesRowView> out = new ArrayList<>(grid.rows().size());
        for (PeriodNotesGridRow r : grid.rows()) {
            List<String> cells = new ArrayList<>();
            for (int i = 0; i < r.averages().size(); i++) {
                cells.add(formatScore(r.averages().get(i)));
            }
            out.add(
                new PeriodNotesRowView(
                    r.lastName() != null ? r.lastName() : "",
                    r.firstName() != null ? r.firstName() : "",
                    cells,
                    formatScore(r.generalAverage())
                )
            );
        }
        return out;
    }

    public record PeriodNotesRowView(String lastName, String firstName, List<String> subjectCells, String generalAverage) {
    }

    private static String formatScore(Double v) {
        if (v == null) {
            return "—";
        }
        return SCORE.format(v);
    }
}
