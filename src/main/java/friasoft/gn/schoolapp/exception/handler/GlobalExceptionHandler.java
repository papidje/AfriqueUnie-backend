package friasoft.gn.schoolapp.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import friasoft.gn.schoolapp.exception.AccountDisabledException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Corps d’erreur homogènes pour l’app Angular : {@code message} et {@code detail} identiques,
 * en plus des champs {@code status} / {@code error}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
        ResponseStatusException ex,
        HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String msg = ex.getReason();
        if (msg == null || msg.isBlank()) {
            msg = defaultMessageForStatus(status);
        }
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildBody(status.value(), status.getReasonPhrase(), msg, request.getRequestURI()));
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<Map<String, Object>> handleAccountDisabled(
        AccountDisabledException ex,
        HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "Ce compte a été désactivé.";
        }
        Map<String, Object> body = buildBody(status.value(), status.getReasonPhrase(), msg, request.getRequestURI());
        body.put("accountDisabled", true);
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
        AccessDeniedException ex,
        HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        String msg = "Accès refusé : vous n’avez pas les droits nécessaires pour cette action.";
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildBody(status.value(), status.getReasonPhrase(), msg, request.getRequestURI()));
    }

    private static Map<String, Object> buildBody(int status, String errorPhrase, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", errorPhrase);
        body.put("message", message);
        body.put("detail", message);
        body.put("path", path);
        return body;
    }

    private static String defaultMessageForStatus(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Requête invalide.";
            case UNAUTHORIZED -> "Authentification requise ou identifiants incorrects.";
            case FORBIDDEN -> "Accès refusé.";
            case NOT_FOUND -> "Ressource introuvable.";
            case CONFLICT -> "Conflit avec l’état actuel des données.";
            default -> status.getReasonPhrase();
        };
    }
}
