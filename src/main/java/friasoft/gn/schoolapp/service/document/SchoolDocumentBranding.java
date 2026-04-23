package friasoft.gn.schoolapp.service.document;

import friasoft.gn.schoolapp.entity.school.School;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

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
import java.util.Base64;
import java.util.Map;

/**
 * Logos d’établissement pour les PDF (en-tête + filigrane), partagé entre les templates.
 */
@Component
public class SchoolDocumentBranding {

    public static final float DEFAULT_WATERMARK_ALPHA = 0.1f;
    private static final int WATERMARK_MAX_PX = 320;
    private static final Path UPLOADS_DIR = Paths.get("./uploads").toAbsolutePath().normalize();

    public void putSchoolOnModel(School school, Map<String, Object> model) {
        if (school == null) {
            return;
        }
        model.put("schoolName", nullSafe(school.getName(), "Établissement"));
        model.put("schoolLogoDataUrl", toImageDataUrl(school.getLogo()));
        model.put("schoolLogoWatermarkDataUrl", toWatermarkPngDataUrl(school.getLogo(), DEFAULT_WATERMARK_ALPHA));
    }

    /** Toute ressource sous /uploads/ (ex. photo élève). */
    public String toImageDataUrl(String relativeUploadPath) {
        if (relativeUploadPath == null || relativeUploadPath.isBlank() || !relativeUploadPath.startsWith("/uploads/")) {
            return null;
        }
        String relative = relativeUploadPath.substring("/uploads/".length());
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

    public String toWatermarkPngDataUrl(String logoPath, float alpha) {
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

    private static String buildWatermarkPngDataUrl(Path filePath, float alpha, int maxDim) throws IOException {
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

    private static String nullSafe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
