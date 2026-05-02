package friasoft.gn.schoolapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Dossier racine des fichiers publics (photos élèves, logos). Aligné sur {@code app.upload.dir} / {@code APP_UPLOAD_DIR}.
 */
@Component
public class AppUploadRoot {

    private final Path root;

    public AppUploadRoot(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public Path getRoot() {
        return root;
    }

    public Path photosPath() {
        return root.resolve("photos");
    }

    public Path logosPath() {
        return root.resolve("logos");
    }
}
