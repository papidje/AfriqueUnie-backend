package friasoft.gn.schoolapp.service.document;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Génération PDF à partir de templates Thymeleaf (HTML/CSS) via OpenHTMLtoPDF.
 */
@Service
@RequiredArgsConstructor
public class PdfService {

    private final TemplateEngine templateEngine;

    public byte[] renderFromTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        String html = templateEngine.process(templateName, context);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de générer le document PDF.", e);
        }
    }
}
