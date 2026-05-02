package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.auth.Activation;
import friasoft.gn.schoolapp.entity.auth.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    /** Durée affichée dans les mails ; alignée sur {@code UserService} (expiration du code). */
    public static final int ACTIVATION_CODE_VALIDITY_MINUTES = 30;

    private final JavaMailSender javaMailSender;
    private final String mailFrom;

    public NotificationService(
        JavaMailSender javaMailSender,
        @Value("${spring.mail.from:no-reply@schoolapp.local}") String mailFrom
    ) {
        this.javaMailSender = javaMailSender;
        this.mailFrom = mailFrom;
    }

    public void sendActivationMail(Activation activation) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(activation.getUser().getEmail());
        message.setSubject("Activez votre compte SchoolApp");

        String body = String.format(
            """
            Bonjour %s,

            Votre code d'activation est : %s

            Ce code est valable %d minutes. Passé ce délai, demandez un nouveau code à votre administrateur ou utilisez la fonction « renvoyer le code » si elle est disponible.

            Cordialement,
            L'équipe SchoolApp
            """,
            activation.getUser().getFullname(),
            activation.getCode(),
            ACTIVATION_CODE_VALIDITY_MINUTES
        );
        message.setText(body);
        javaMailSender.send(message);
        log.info("Mail d'activation envoyé à {}", activation.getUser().getEmail());
    }

    public void sendResetPassWordMail(Activation activation) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(activation.getUser().getEmail());
        message.setSubject("Réinitialisation de votre mot de passe");

        String body = String.format(
            """
            Bonjour %s,

            Votre code de réinitialisation du mot de passe est : %s

            Ce code est valable %d minutes.

            Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.

            Cordialement,
            L'équipe SchoolApp
            """,
            activation.getUser().getFullname(),
            activation.getCode(),
            ACTIVATION_CODE_VALIDITY_MINUTES
        );
        message.setText(body);
        javaMailSender.send(message);
        log.info("Mail de réinitialisation envoyé à {}", activation.getUser().getEmail());
    }

    public void sendAccountActivatedMail(User user) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Votre compte SchoolApp est activé");

        String body = String.format(
            """
            Bonjour %s,

            Votre compte a bien été activé. Vous pouvez désormais vous connecter avec l'adresse e-mail utilisée à l'inscription et le mot de passe que vous avez choisi.

            Cordialement,
            L'équipe SchoolApp
            """,
            user.getFullname()
        );
        message.setText(body);
        javaMailSender.send(message);
        log.info("Mail de confirmation d'activation envoyé à {}", user.getEmail());
    }

    public void sendPasswordChangedConfirmationMail(User user) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Votre mot de passe SchoolApp a été modifié");

        String body = String.format(
            """
            Bonjour %s,

            Le mot de passe de votre compte SchoolApp vient d'être modifié.

            Si vous n'êtes pas à l'origine de ce changement, contactez immédiatement l'administrateur de votre établissement.

            Cordialement,
            L'équipe SchoolApp
            """,
            user.getFullname()
        );
        message.setText(body);
        javaMailSender.send(message);
        log.info("Mail de confirmation de changement de mot de passe envoyé à {}", user.getEmail());
    }
}
