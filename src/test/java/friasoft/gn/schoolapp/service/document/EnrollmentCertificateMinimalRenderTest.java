package friasoft.gn.schoolapp.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Vérifie que l’attestation d’inscription se génère avec le jeu minimal
 * (pas de logo, pas de photo, date/lieu de naissance absents).
 */
class EnrollmentCertificateMinimalRenderTest {

    private PdfService pdfService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        pdfService = new PdfService(engine);
    }

    @Test
    void generatesPdfWithMinimalStudentFields() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("schoolName", "École Test");
        vars.put("schoolLogoDataUrl", null);
        vars.put("schoolLogoWatermarkDataUrl", null);
        vars.put("studentFullName", "CAMARA Aissatou");
        vars.put("studentPhotoDataUrl", null);
        vars.put("studentMatricule", "120260401234");
        vars.put("className", "6ème A");
        vars.put("schoolYearLabel", "2025-2026");
        vars.put("birthDate", "—");
        vars.put("birthPlace", "—");
        vars.put("issueDate", DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE).format(LocalDate.now()));

        byte[] pdf = assertDoesNotThrow(
            () -> pdfService.renderFromTemplate("documents/attestation-inscription", vars)
        );

        assertTrue(pdf != null && pdf.length > 100, "PDF attendu non vide");
        assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F', "En-tête %PDF attendu");
    }

    @Test
    void generatesPdfWhenOptionalStringsAreEmpty() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("schoolName", "École");
        vars.put("schoolLogoDataUrl", "");
        vars.put("schoolLogoWatermarkDataUrl", "");
        vars.put("studentFullName", "DIALLO Mamadou");
        vars.put("studentPhotoDataUrl", "");
        vars.put("studentMatricule", "—");
        vars.put("className", "CP1");
        vars.put("schoolYearLabel", "2025-2026");
        vars.put("birthDate", "—");
        vars.put("birthPlace", "—");
        vars.put("issueDate", "06/10/2026");

        byte[] pdf = assertDoesNotThrow(
            () -> pdfService.renderFromTemplate("documents/attestation-inscription", vars)
        );

        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, 0, 4).equals("%PDF"));
    }
}
