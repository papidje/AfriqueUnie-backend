package friasoft.gn.schoolapp.service.document;

import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StudentDocumentService {

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);

    private final IStudentRepository studentRepository;
    private final SchoolService schoolService;
    private final SchoolDocumentBranding schoolDocumentBranding;
    private final PdfService pdfService;

    public byte[] generateEnrollmentCertificate(Long studentId) {
        Student student = studentRepository.findByIdWithParentsAndClass(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Élève introuvable."));
        if (student.getSchoolClass() == null || student.getSchoolClass().getYear() == null
            || student.getSchoolClass().getYear().getSchool() == null) {
            throw new IllegalArgumentException("Contexte classe/école introuvable.");
        }

        School school = student.getSchoolClass().getYear().getSchool();
        schoolService.assertCurrentUserCanAccessSchool(school.getId());

        Map<String, Object> vars = new HashMap<>();
        schoolDocumentBranding.putSchoolOnModel(school, vars);
        vars.put("studentFullName", (nullSafe(student.getLastName(), "") + " " + nullSafe(student.getFirstName(), "")).trim());
        vars.put("studentPhotoDataUrl", schoolDocumentBranding.toImageDataUrl(student.getPhotoPath()));
        vars.put("studentMatricule", nullSafe(student.getMatricule(), "—"));
        vars.put("className", student.getSchoolClass() != null ? nullSafe(student.getSchoolClass().getName(), "—") : "—");
        vars.put("schoolYearLabel", student.getSchoolClass().getYear() != null
            ? nullSafe(student.getSchoolClass().getYear().getLabel(), "—")
            : "—");
        vars.put("birthDate", student.getBirthDate() != null ? DATE_FR.format(student.getBirthDate()) : "—");
        vars.put("birthPlace", nullSafe(student.getBirthPlace(), "—"));
        vars.put("issueDate", DATE_FR.format(LocalDate.now()));

        return pdfService.renderFromTemplate("documents/attestation-inscription", vars);
    }

    private static String nullSafe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
