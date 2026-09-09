package friasoft.gn.schoolapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * URLs absolues vers la SPA (activation, connexion, etc.) pour les e-mails transactionnels.
 * Surcharge : {@code APP_FRONTEND_BASE_URL} / {@code app.frontend.base-url}.
 */
@Component
public class AppFrontendLinks {

    private final String baseUrl;

    public AppFrontendLinks(@Value("${app.frontend.base-url:https://karanso.com}") String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        this.baseUrl = trimmed.isEmpty() ? "https://karanso.com" : trimmed;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String login() {
        return baseUrl + "/login";
    }

    public String notifications() {
        return baseUrl + "/notifications";
    }

    public String contact() {
        return baseUrl + "/contact";
    }

    public String activate(String email) {
        return activate(email, null);
    }

    public String activate(String email, String activationCode) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(baseUrl + "/activate");
        if (email != null && !email.isBlank()) {
            b.queryParam("email", email.trim());
        }
        if (activationCode != null && !activationCode.isBlank()) {
            b.queryParam("code", activationCode.trim());
        }
        return b.build().encode().toUriString();
    }

    public String newPassword(String email) {
        return newPassword(email, null);
    }

    public String newPassword(String email, String resetCode) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(baseUrl + "/newPwd");
        if (email != null && !email.isBlank()) {
            b.queryParam("email", email.trim());
        }
        if (resetCode != null && !resetCode.isBlank()) {
            b.queryParam("code", resetCode.trim());
        }
        return b.build().encode().toUriString();
    }

    /** Lien HTML cliquable (libellé + URL échappés pour attribut / texte). */
    public static String htmlAnchor(String url, String label) {
        String safeUrl = escapeAttr(url);
        String safeLabel = escapeText(label);
        return "<a href=\"" + safeUrl + "\">" + safeLabel + "</a>";
    }

    private static String escapeAttr(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private static String escapeText(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }
}
