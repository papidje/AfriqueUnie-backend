package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.Activation;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class NotificationService {
    @Autowired
    private JavaMailSender javaMailSender;

    public void sendActivationMail(Activation activation) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("papidje@yopmail.com");
        message.setTo(activation.getUser().getEmail());
        message.setSubject("Activation Mail");

        String body = String.format(
                "Bonjour %s, <br /> Votre code d'activation est %s. <br /> A bientôt.",
                activation.getUser().getFullname(), activation.getCode()
        );
        message.setText(body);
//        javaMailSender.send(message);
    }

    public void sendResetPassWordMail(Activation activation) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("papidje@yopmail.com");
        message.setTo(activation.getUser().getEmail());
        message.setSubject("Activation Mail");

        String body = String.format(
                "Bonjour %s, <br /> Votre code de réinitialisation de mot de pass est %s. <br /> A bientôt.",
                activation.getUser().getFullname(), activation.getCode()
        );
        message.setText(body);
//        javaMailSender.send(message);
    }
}
