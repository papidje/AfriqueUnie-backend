package friasoft.gn.schoolapp.storage;

import friasoft.gn.schoolapp.repository.IStudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentPhotoCleanupService {

    private static final Path PHOTOS_DIR = Paths.get("./uploads/photos").toAbsolutePath().normalize();
    private static final String PHOTO_WEB_PREFIX = "/uploads/photos/";

    private final IStudentRepository studentRepository;

    /** Nettoyage quotidien des photos orphelines à 03:00. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional(readOnly = true)
    public void cleanupOrphanPhotos() {
        try {
            Files.createDirectories(PHOTOS_DIR);
        } catch (IOException e) {
            log.warn("Impossible d'initialiser le dossier photos: {}", e.getMessage());
            return;
        }

        Set<String> referencedNames = studentRepository.findDistinctPhotoPaths().stream()
            .map(this::extractFilename)
            .filter(name -> name != null && !name.isBlank())
            .collect(Collectors.toSet());

        int deleted = 0;
        try (Stream<Path> stream = Files.list(PHOTOS_DIR)) {
            for (Path file : stream.toList()) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                String filename = file.getFileName().toString();
                if (referencedNames.contains(filename)) {
                    continue;
                }
                try {
                    Files.deleteIfExists(file);
                    deleted++;
                } catch (IOException ex) {
                    log.warn("Suppression impossible pour {}: {}", filename, ex.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Lecture impossible du dossier photos: {}", e.getMessage());
            return;
        }

        if (deleted > 0) {
            log.info("Nettoyage photos orphelines terminé: {} fichier(s) supprimé(s).", deleted);
        }
    }

    private String extractFilename(String path) {
        if (path == null || path.isBlank() || !path.startsWith(PHOTO_WEB_PREFIX)) {
            return null;
        }
        String filename = path.substring(PHOTO_WEB_PREFIX.length());
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return null;
        }
        return filename;
    }
}
