package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.Parent;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.service.document.PaymentReceiptPdfService;
import friasoft.gn.schoolapp.service.document.SchoolDocumentBranding;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * E-mail de confirmation de paiement aux parents (HTML inline + reçu PDF en pièce jointe).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConfirmationMailService {

    private static final NumberFormat GNF = NumberFormat.getNumberInstance(Locale.FRANCE);

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final PaymentReceiptPdfService paymentReceiptPdfService;
    private final SchoolDocumentBranding schoolDocumentBranding;

    @Value("${spring.mail.from:no-reply@schoolapp.local}")
    private String mailFrom;

    /**
     * Envoie l’e-mail à chaque adresse parent distincte. Les erreurs sont journalisées sans lever d’exception.
     */
    public void sendPaymentConfirmation(Student student, String receiptReference, double totalCollected) {
        if (student == null || !StringUtils.hasText(receiptReference)) {
            return;
        }
        if (student.getSchoolClass() == null || student.getSchoolClass().getYear() == null
            || student.getSchoolClass().getYear().getSchool() == null) {
            log.debug("Confirmation mail: contexte école manquant pour élève {}", student.getId());
            return;
        }
        try {
            School school = student.getSchoolClass().getYear().getSchool();
            List<ParentRecipient> recipients = buildParentRecipients(student);
            if (recipients.isEmpty()) {
                log.debug("Aucun e-mail parent pour confirmation paiement (élève {}).", student.getId());
                return;
            }
            byte[] pdf = paymentReceiptPdfService.buildReceiptPdf(student.getId(), receiptReference.trim());
            String safeFileBase = safeAttachmentBase(receiptReference);
            String attachmentName = "reçu-" + safeFileBase + ".pdf";

            String subject = "Confirmation de paiement — " + (school.getName() != null ? school.getName() : "Établissement scolaire");
            String studentName = (student.getLastName() + " " + student.getFirstName()).trim();
            if (studentName.isBlank()) {
                studentName = "votre enfant";
            }
            String amountText = GNF.format(Math.round(totalCollected));

            for (ParentRecipient to : recipients) {
                String html = buildHtmlBody(
                    school,
                    to.greeting(),
                    amountText,
                    studentName
                );
                sendMessage(to.address(), subject, html, pdf, attachmentName);
                log.info("Mail confirmation paiement envoyé à {} (reçu {})", to.address(), receiptReference);
            }
        } catch (Exception e) {
            log.warn("Envoi de la confirmation de paiement par e-mail annulé ou en échec (élève {}): {}", student.getId(), e.getMessage());
        }
    }

    private void sendMessage(String to, String subject, String html, byte[] pdf, String attachmentName) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        helper.addAttachment(attachmentName, new ByteArrayResource(pdf), "application/pdf");
        javaMailSender.send(message);
    }

    private String buildHtmlBody(School school, String parentGreeting, String amountText, String studentName) {
        Map<String, Object> m = new LinkedHashMap<>();
        schoolDocumentBranding.putSchoolOnModel(school, m);
        String logo = (String) m.get("schoolLogoDataUrl");
        m.put("hasLogo", StringUtils.hasText(logo));
        m.put("schoolName", m.get("schoolName") != null ? m.get("schoolName") : "Établissement");
        m.put("parentGreeting", parentGreeting);
        m.put("amountText", amountText);
        m.put("studentName", studentName);
        m.put("schoolAddress", nullToEmpty(school.getAdress()));
        m.put("schoolContact", nullToEmpty(school.getContact()));
        m.put("hasAddress", StringUtils.hasText(school.getAdress()));
        m.put("hasContact", StringUtils.hasText(school.getContact()));

        Context context = new Context(Locale.FRANCE);
        context.setVariables(m);
        return templateEngine.process("emails/payment-confirmation", context);
    }

    private static String nullToEmpty(@Nullable String s) {
        return s != null && !s.isBlank() ? s.trim() : "";
    }

    private static String safeAttachmentBase(String ref) {
        return ref.trim().replaceAll("[^A-Za-z0-9._-]+", "-");
    }

    private static String parentDisplayName(Parent p) {
        if (p == null) {
            return "";
        }
        return (p.getFirstName() + " " + p.getLastName()).trim();
    }

    private static String normalizeEmailKey(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Une entrée par adresse distincte. Regroupe père, mère et tuteur (e-mail
     * {@link Student#getTutorEmail()} / {@link Student#getTutorName()}) : si
     * plusieurs rôles partagent la même adresse, les prénoms sont joints par « et ».
     */
    private static List<ParentRecipient> buildParentRecipients(Student student) {
        Map<String, RecipientGroup> byKey = new LinkedHashMap<>();
        mergeRecipient(byKey, student.getFather());
        mergeRecipient(byKey, student.getMother());
        mergeTutor(byKey, student);
        List<ParentRecipient> out = new ArrayList<>();
        for (RecipientGroup g : byKey.values()) {
            if (!isPlausibleEmail(g.getAddress())) {
                continue;
            }
            String greeting = g.buildGreeting();
            out.add(new ParentRecipient(g.getAddress().trim(), greeting));
        }
        return out;
    }

    private static void mergeRecipient(Map<String, RecipientGroup> byKey, @Nullable Parent p) {
        if (p == null || !StringUtils.hasText(p.getEmail())) {
            return;
        }
        String email = p.getEmail().trim();
        if (!isPlausibleEmail(email)) {
            return;
        }
        String key = normalizeEmailKey(email);
        byKey.computeIfAbsent(key, k -> new RecipientGroup(email));
        byKey.get(key).addName(parentDisplayName(p));
    }

    private static void mergeTutor(Map<String, RecipientGroup> byKey, Student student) {
        if (student == null || !StringUtils.hasText(student.getTutorEmail())) {
            return;
        }
        String email = student.getTutorEmail().trim();
        if (!isPlausibleEmail(email)) {
            return;
        }
        String key = normalizeEmailKey(email);
        String display = StringUtils.hasText(student.getTutorName()) ? student.getTutorName().trim() : "";
        if (display.isBlank()) {
            display = " ";
        }
        byKey.computeIfAbsent(key, k -> new RecipientGroup(email));
        byKey.get(key).addName(display);
    }

    private static final class RecipientGroup {
        private final String address;
        private final List<String> names = new ArrayList<>();

        RecipientGroup(String address) {
            this.address = address;
        }

        String getAddress() {
            return address;
        }

        void addName(String name) {
            names.add(StringUtils.hasText(name) ? name : " ");
        }

        String buildGreeting() {
            if (names.isEmpty()) {
                return " ";
            }
            if (names.size() == 1) {
                String g = names.get(0);
                return (g == null || g.isBlank()) ? " " : g;
            }
            return names.stream()
                .filter(n -> n != null && !n.isBlank() && !" ".equals(n))
                .reduce((a, b) -> a + " et " + b)
                .orElse(" ");
        }
    }

    private static boolean isPlausibleEmail(String s) {
        if (s == null || s.length() < 5) {
            return false;
        }
        int at = s.indexOf('@');
        return at > 0 && at < s.length() - 1 && s.indexOf('@', at + 1) < 0;
    }

    private record ParentRecipient(String address, String greeting) {
    }
}
