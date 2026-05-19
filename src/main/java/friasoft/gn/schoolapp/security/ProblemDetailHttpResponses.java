package friasoft.gn.schoolapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;

/**
 * Erreurs JSON depuis le filtre JWT ou la chaîne Security (hors {@code DispatcherServlet}).
 * Inclut à la fois {@code message} et {@code detail} pour les clients qui ne lisent qu’un des deux.
 */
final class ProblemDetailHttpResponses {

    private ProblemDetailHttpResponses() {}

    static void writeUnauthorized(HttpServletResponse response, ObjectMapper objectMapper, String message)
        throws IOException {
        writeJsonError(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", message);
    }

    static void writeForbidden(HttpServletResponse response, ObjectMapper objectMapper, String message)
        throws IOException {
        writeJsonError(response, objectMapper, HttpServletResponse.SC_FORBIDDEN, "Forbidden", message);
    }

    /** Compte désactivé : le front peut rediriger vers une page dédiée sans traiter comme un simple 403 métier. */
    static void writeForbiddenAccountDisabled(HttpServletResponse response, ObjectMapper objectMapper, String message)
        throws IOException {
        writeJsonError(
            response,
            objectMapper,
            HttpServletResponse.SC_FORBIDDEN,
            "Forbidden",
            message,
            Map.of("accountDisabled", Boolean.TRUE)
        );
    }

    private static void writeJsonError(
        HttpServletResponse response,
        ObjectMapper objectMapper,
        int status,
        String errorPhrase,
        String message
    ) throws IOException {
        writeJsonError(response, objectMapper, status, errorPhrase, message, null);
    }

    private static void writeJsonError(
        HttpServletResponse response,
        ObjectMapper objectMapper,
        int status,
        String errorPhrase,
        String message,
        Map<String, Object> extra
    ) throws IOException {
        response.resetBuffer();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", errorPhrase);
        body.put("message", message);
        body.put("detail", message);
        if (extra != null && !extra.isEmpty()) {
            body.putAll(extra);
        }
        objectMapper.writeValue(response.getOutputStream(), body);
        response.flushBuffer();
    }
}
