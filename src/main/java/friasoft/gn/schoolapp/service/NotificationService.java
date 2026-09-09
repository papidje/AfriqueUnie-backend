package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.config.AppFrontendLinks;
import friasoft.gn.schoolapp.entity.auth.Activation;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.service.communication.CommunicationMailDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Service
public class NotificationService {

    /** Durée affichée dans les mails ; alignée sur {@code UserService} (expiration du code). */
    public static final int ACTIVATION_CODE_VALIDITY_MINUTES = 30;

    private final CommunicationMailDispatchService mailDispatchService;
    private final AppFrontendLinks frontendLinks;

    public NotificationService(
        CommunicationMailDispatchService mailDispatchService,
        AppFrontendLinks frontendLinks
    ) {
        this.mailDispatchService = mailDispatchService;
        this.frontendLinks = frontendLinks;
    }

    public void sendActivationMail(Activation activation) {
        User user = activation.getUser();
        String email = user.getEmail();
        String activateUrl = frontendLinks.activate(email, activation.getCode());
        String subject = "Activez votre compte Karanso";
        String html =
            """
            <html><body style="font-family:sans-serif;font-size:14px;line-height:1.5;color:#222;">
            <p>Bonjour %s,</p>
            <p>Votre code d'activation est : <strong>%s</strong></p>
            <p>Ce code est valable %d minutes. Passé ce délai, demandez un nouveau code à votre administrateur ou utilisez la fonction « renvoyer le code » si elle est disponible.</p>
            <p>Pour activer votre compte, ouvrez la page suivante (le code et l'e-mail seront préremplis) :<br/>
            %s</p>
            <p>Cordialement,<br/>L'équipe Karanso</p>
            </body></html>
            """
                .formatted(
                    HtmlUtils.htmlEscape(nullToEmpty(user.getFullname())),
                    HtmlUtils.htmlEscape(nullToEmpty(activation.getCode())),
                    ACTIVATION_CODE_VALIDITY_MINUTES,
                    AppFrontendLinks.htmlAnchor(activateUrl, activateUrl)
                );
        sendHtmlBestEffort(email, subject, html, "activation");
    }

    public void sendResetPassWordMail(Activation activation) {
        User user = activation.getUser();
        String email = user.getEmail();
        String resetUrl = frontendLinks.newPassword(email, activation.getCode());
        String subject = "Réinitialisation de votre mot de passe";
        String html =
            """
            <html><body style="font-family:sans-serif;font-size:14px;line-height:1.5;color:#222;">
            <p>Bonjour %s,</p>
            <p>Votre code de réinitialisation du mot de passe est : <strong>%s</strong></p>
            <p>Ce code est valable %d minutes.</p>
            <p>Pour choisir un nouveau mot de passe, ouvrez la page suivante :<br/>
            %s</p>
            <p>Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.</p>
            <p>Cordialement,<br/>L'équipe Karanso</p>
            </body></html>
            """
                .formatted(
                    HtmlUtils.htmlEscape(nullToEmpty(user.getFullname())),
                    HtmlUtils.htmlEscape(nullToEmpty(activation.getCode())),
                    ACTIVATION_CODE_VALIDITY_MINUTES,
                    AppFrontendLinks.htmlAnchor(resetUrl, resetUrl)
                );
        sendHtmlBestEffort(email, subject, html, "réinitialisation");
    }

    public void sendAccountActivatedMail(User user) {
        String loginUrl = frontendLinks.login();
        String subject = "Votre compte Karanso est activé";
        String html =
            """
            <html><body style="font-family:sans-serif;font-size:14px;line-height:1.5;color:#222;">
            <p>Bonjour %s,</p>
            <p>Votre compte a bien été activé. Vous pouvez désormais vous connecter avec l'adresse e-mail utilisée à l'inscription et le mot de passe que vous avez choisi.</p>
            <p>Se connecter :<br/>%s</p>
            <p>Cordialement,<br/>L'équipe Karanso</p>
            </body></html>
            """
                .formatted(
                    HtmlUtils.htmlEscape(nullToEmpty(user.getFullname())),
                    AppFrontendLinks.htmlAnchor(loginUrl, loginUrl)
                );
        sendHtmlBestEffort(user.getEmail(), subject, html, "confirmation d'activation");
    }

    public void sendPasswordChangedConfirmationMail(User user) {
        String loginUrl = frontendLinks.login();
        String subject = "Votre mot de passe Karanso a été modifié";
        String html =
            """
            <html><body style="font-family:sans-serif;font-size:14px;line-height:1.5;color:#222;">
            <p>Bonjour %s,</p>
            <p>Le mot de passe de votre compte Karanso vient d'être modifié.</p>
            <p>Si vous n'êtes pas à l'origine de ce changement, contactez immédiatement l'administrateur de votre établissement.</p>
            <p>Se connecter :<br/>%s</p>
            <p>Cordialement,<br/>L'équipe Karanso</p>
            </body></html>
            """
                .formatted(
                    HtmlUtils.htmlEscape(nullToEmpty(user.getFullname())),
                    AppFrontendLinks.htmlAnchor(loginUrl, loginUrl)
                );
        sendHtmlBestEffort(user.getEmail(), subject, html, "confirmation de changement de mot de passe");
    }

    /**
     * Compte déjà actif : information de rattachement à un ou plusieurs établissements (sans code d’activation).
     */
    public void sendSchoolAffiliationAttachedNotice(User user, java.util.List<String> schoolNames) {
        if (schoolNames == null || schoolNames.isEmpty()) {
            return;
        }

        String liste = String.join(", ", schoolNames);
        String intro =
            schoolNames.size() == 1
                ? "Vous avez été rattaché à un nouvel établissement : " + HtmlUtils.htmlEscape(liste) + "."
                : "Vous avez été rattaché à de nouveaux établissements : " + HtmlUtils.htmlEscape(liste) + ".";
        String loginUrl = frontendLinks.login();
        String subject = "Nouveau rattachement à un établissement";
        String html =
            """
            <html><body style="font-family:sans-serif;font-size:14px;line-height:1.5;color:#222;">
            <p>Bonjour %s,</p>
            <p>%s</p>
            <p>Connectez-vous à Karanso pour accéder à votre espace :<br/>%s</p>
            <p>Cordialement,<br/>L'équipe Karanso</p>
            </body></html>
            """
                .formatted(
                    HtmlUtils.htmlEscape(nullToEmpty(user.getFullname())),
                    intro,
                    AppFrontendLinks.htmlAnchor(loginUrl, loginUrl)
                );
        sendHtmlBestEffort(user.getEmail(), subject, html, "rattachement établissement(s)");
    }

    private void sendHtmlBestEffort(String to, String subject, String html, String kind) {
        if (to == null || to.isBlank()) {
            log.warn("Envoi mail {} ignoré : destinataire vide", kind);
            return;
        }
        try {
            mailDispatchService.sendHtml(to.trim(), subject, html);
            log.info("Mail {} envoyé à {}", kind, to);
        } catch (Exception ex) {
            log.warn("Échec envoi mail {} à {} : {}", kind, to, ex.getMessage());
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
