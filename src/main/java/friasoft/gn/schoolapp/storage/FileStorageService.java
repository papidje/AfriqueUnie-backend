package friasoft.gn.schoolapp.storage;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Path PHOTOS_DIR = Paths.get("./uploads/photos").toAbsolutePath().normalize();
    private static final String PHOTO_WEB_PREFIX = "/uploads/photos/";

    private static final Path LOGOS_DIR = Paths.get("./uploads/logos").toAbsolutePath().normalize();
    private static final String LOGO_WEB_PREFIX = "/uploads/logos/";
    private static final int SCHOOL_LOGO_MAX_SIDE = 500;

    public String storeStudentPhoto(Long studentId, MultipartFile photo) {
        return storeStudentPhoto(studentId, photo, null);
    }

    public String storeStudentPhoto(Long studentId, MultipartFile photo, String previousPhotoPath) {
        if (photo == null || photo.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier photo transmis.");
        }
        try {
            Files.createDirectories(PHOTOS_DIR);
            String filename = "student-" + studentId + "-" + UUID.randomUUID().toString().replace("-", "") + ".jpg";
            Path target = PHOTOS_DIR.resolve(filename).normalize();
            Path temp = Files.createTempFile("student-photo-", ".tmp");
            Files.copy(photo.getInputStream(), temp, StandardCopyOption.REPLACE_EXISTING);
            Thumbnails.of(temp.toFile())
                .size(200, 200)
                .outputFormat("jpg")
                .outputQuality(0.9)
                .toFile(target.toFile());
            Files.deleteIfExists(temp);
            deletePreviousPhoto(previousPhotoPath);
            return PHOTO_WEB_PREFIX + filename;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'enregistrer la photo.", e);
        }
    }

    private void deletePreviousPhoto(String previousPhotoPath) {
        if (previousPhotoPath == null || previousPhotoPath.isBlank()) {
            return;
        }
        if (!previousPhotoPath.startsWith(PHOTO_WEB_PREFIX)) {
            return;
        }
        String filename = previousPhotoPath.substring(PHOTO_WEB_PREFIX.length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return;
        }
        Path previous = PHOTOS_DIR.resolve(filename).normalize();
        if (!previous.getParent().equals(PHOTOS_DIR)) {
            return;
        }
        try {
            Files.deleteIfExists(previous);
        } catch (IOException ignored) {
            // Le remplacement de photo doit rester non bloquant si l'ancien fichier est déjà absent/verrouillé.
        }
    }

    public String storeSchoolLogo(Long schoolId, MultipartFile logo) {
        return storeSchoolLogo(schoolId, logo, null);
    }

    public String storeSchoolLogo(Long schoolId, MultipartFile logo, String previousLogoPath) {
        if (logo == null || logo.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier logo transmis.");
        }
        try {
            Files.createDirectories(LOGOS_DIR);
            String filename = "school-" + schoolId + "-" + UUID.randomUUID().toString().replace("-", "") + ".jpg";
            Path target = LOGOS_DIR.resolve(filename).normalize();
            Path temp = Files.createTempFile("school-logo-", ".tmp");
            Files.copy(logo.getInputStream(), temp, StandardCopyOption.REPLACE_EXISTING);
            Thumbnails.of(temp.toFile())
                .size(SCHOOL_LOGO_MAX_SIDE, SCHOOL_LOGO_MAX_SIDE)
                .outputFormat("jpg")
                .outputQuality(0.9)
                .toFile(target.toFile());
            Files.deleteIfExists(temp);
            deletePreviousLogo(previousLogoPath);
            return LOGO_WEB_PREFIX + filename;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'enregistrer le logo.", e);
        }
    }

    private void deletePreviousLogo(String previousLogoPath) {
        if (previousLogoPath == null || previousLogoPath.isBlank()) {
            return;
        }
        if (!previousLogoPath.startsWith(LOGO_WEB_PREFIX)) {
            return;
        }
        String filename = previousLogoPath.substring(LOGO_WEB_PREFIX.length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return;
        }
        Path previous = LOGOS_DIR.resolve(filename).normalize();
        if (!previous.getParent().equals(LOGOS_DIR)) {
            return;
        }
        try {
            Files.deleteIfExists(previous);
        } catch (IOException ignored) {
        }
    }
}
