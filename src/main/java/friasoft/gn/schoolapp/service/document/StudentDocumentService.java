package friasoft.gn.schoolapp.service.document;

import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StudentDocumentService {

    private static final Path UPLOADS_DIR = Paths.get("./uploads").toAbsolutePath().normalize();
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);
    private static final int WATERMARK_MAX_PX = 320;
    /** Opacité du filigrane (l’image est prémultipliée côté serveur ; max 1.0). */
    private static final float WATERMARK_ALPHA = 0.1f;

    private final IStudentRepository studentRepository;
    private final SchoolService schoolService;
    private final PdfTemplateRenderer pdfTemplateRenderer;

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
        vars.put("schoolName", nullSafe(school.getName(), "École"));
        vars.put("schoolLogoDataUrl", toImageDataUrl(school.getLogo()));
        vars.put("schoolLogoWatermarkDataUrl", toWatermarkPngDataUrl(school.getLogo(), WATERMARK_ALPHA));
        vars.put("studentFullName", (nullSafe(student.getLastName(), "") + " " + nullSafe(student.getFirstName(), "")).trim());
        vars.put("studentPhotoDataUrl", toImageDataUrl(student.getPhotoPath()));
        vars.put("studentMatricule", nullSafe(student.getMatricule(), "—"));
        vars.put("className", student.getSchoolClass() != null ? nullSafe(student.getSchoolClass().getName(), "—") : "—");
        vars.put("schoolYearLabel", student.getSchoolClass().getYear() != null
            ? nullSafe(student.getSchoolClass().getYear().getLabel(), "—")
            : "—");
        vars.put("birthDate", student.getBirthDate() != null ? DATE_FR.format(student.getBirthDate()) : "—");
        vars.put("birthPlace", nullSafe(student.getBirthPlace(), "—"));
        vars.put("issueDate", DATE_FR.format(LocalDate.now()));

        return pdfTemplateRenderer.render("documents/attestation-inscription", vars);
    }

    private String toImageDataUrl(String logoPath) {
        if (logoPath == null || logoPath.isBlank() || !logoPath.startsWith("/uploads/")) {
            return null;
        }
        String relative = logoPath.substring("/uploads/".length());
        Path path = UPLOADS_DIR.resolve(relative).normalize();
        if (!path.startsWith(UPLOADS_DIR) || !Files.exists(path)) {
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            String mime = Files.probeContentType(path);
            if (mime == null || mime.isBlank()) {
                mime = "image/jpeg";
            }
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Image PNG en data-URL, déjà atténuée (alpha) pour le filigrane.
     */
    private String toWatermarkPngDataUrl(String logoPath, float alpha) {
        if (logoPath == null || logoPath.isBlank() || !logoPath.startsWith("/uploads/")) {
            return null;
        }
        String relative = logoPath.substring("/uploads/".length());
        Path path = UPLOADS_DIR.resolve(relative).normalize();
        if (!path.startsWith(UPLOADS_DIR) || !Files.exists(path)) {
            return null;
        }
        try {
            String png = buildWatermarkPngDataUrl(path, alpha, WATERMARK_MAX_PX);
            if (png != null) {
                return png;
            }
        } catch (Exception ignored) {
        }
        return toImageDataUrl(logoPath);
    }

    /**
     * Charge une image de façon plus tolérante que seul {@link ImageIO#read} (JPEG progressifs, etc.) :
     * d’abord ImageIO, puis Thumbnailator sur fichier ou sur flux.
     */
    private static BufferedImage readBufferedImage(Path path, byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            return null;
        }
        try {
            BufferedImage bi = ImageIO.read(path.toFile());
            if (bi != null) {
                return bi;
            }
        } catch (Exception ignored) {
        }
        try {
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(fileBytes));
            if (bi != null) {
                return bi;
            }
        } catch (Exception ignored) {
        }
        try {
            return Thumbnails.of(path.toFile()).scale(1.0).asBufferedImage();
        } catch (Exception ignored) {
        }
        try (InputStream is = new ByteArrayInputStream(fileBytes)) {
            return Thumbnails.of(is).scale(1.0).asBufferedImage();
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildWatermarkPngDataUrl(Path filePath, float alpha, int maxDim) throws IOException {
        if (filePath == null) {
            return null;
        }
        byte[] fileBytes = Files.readAllBytes(filePath);
        if (fileBytes.length == 0) {
            return null;
        }
        BufferedImage src = readBufferedImage(filePath, fileBytes);
        if (src == null) {
            return null;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        double scale = Math.min(1.0, (double) maxDim / (double) Math.max(w, h));
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setComposite(
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.clamp(alpha, 0.02f, 1.0f))
            );
            g2.drawImage(src, 0, 0, nw, nh, null);
        } finally {
            g2.dispose();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!ImageIO.write(out, "png", baos)) {
            throw new IOException("Écriture PNG refusée (ImageIO.write).");
        }
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private static String nullSafe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
