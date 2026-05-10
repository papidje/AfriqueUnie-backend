package friasoft.gn.schoolapp.service.communication;

import friasoft.gn.schoolapp.dto.StudentPaymentStatusDTO;
import friasoft.gn.schoolapp.entity.school.NotificationBatchSettings;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.service.FinanceService;
import friasoft.gn.schoolapp.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPaymentReminderJob {

    private final ISchoolClassRepository schoolClassRepository;
    private final FinanceService financeService;
    private final IStudentRepository studentRepository;
    private final ParentDeliveryResolver parentDeliveryResolver;
    private final NotificationDispatchAssistant notificationDispatchAssistant;

    @Transactional(readOnly = true)
    public void run(NotificationBatchSettings settings) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null || !Boolean.TRUE.equals(settings.getPaymentReminderEnabled())) {
            return;
        }
        boolean mail = Boolean.TRUE.equals(settings.getEmailEnabled());
        boolean sms = Boolean.TRUE.equals(settings.getSmsEnabled());
        if (!mail && !sms) {
            return;
        }

        long paymentDedupeRef = LocalDate.now(ZoneId.systemDefault()).toEpochDay();

        List<Long> classIds = schoolClassRepository.findIdsForActiveSchoolYears();
        for (Long classId : classIds) {
            Long schoolId = schoolClassRepository.findByIdWithYearAndSchool(classId)
                .map(sc -> sc.getYear().getSchool().getId())
                .orElse(null);
            if (schoolId == null) {
                log.debug("Batch paiement : école introuvable pour la classe {}", classId);
                continue;
            }
            List<StudentPaymentStatusDTO> rows;
            try {
                rows = financeService.getClassPaymentStatusTrusted(classId);
            } catch (Exception ex) {
                log.debug("Batch paiement : classe {} ignorée ({})", classId, ex.getMessage());
                continue;
            }
            for (StudentPaymentStatusDTO row : rows) {
                if (row.totalExpected() == null || row.totalExpected() <= 1e-6) {
                    continue;
                }
                double pct = row.paymentPercentage() != null ? row.paymentPercentage() : 0d;
                if (pct >= 99.99d) {
                    continue;
                }
                Student student = studentRepository.findById(row.studentId()).orElse(null);
                if (student == null) {
                    continue;
                }
                student = studentRepository.findByIdWithParentsAndClass(student.getId()).orElse(student);

                String template = """
                    <p>%GREETING%</p>
                    <p>Nous vous informons qu’un solde reste dû pour la scolarité de <strong>%FN% %LN%</strong>.</p>
                    <p>Merci de régulariser ou de contacter l’administration pour toute question.</p>
                    <p>Cordialement,<br/>Comptabilité scolaire</p>
                    """;
                String smsText = "Relance paiement scolarité pour " + student.getFirstName() + " " + student.getLastName()
                    + ". Merci de contacter l'établissement.";
                String subject = "Relance paiement — solde scolarité";
                for (ParentDeliveryResolver.ParentDeliveryTarget t : parentDeliveryResolver.resolveDistinctEmailTargets(student)) {
                    String body = template
                        .replace("%GREETING%", escape(t.greetingKey()))
                        .replace("%FN%", escape(student.getFirstName()))
                        .replace("%LN%", escape(student.getLastName()));
                    notificationDispatchAssistant.sendParentTargetIfNew(
                        tenantId,
                        schoolId,
                        CommunicationEventType.PAYMENT_OVERDUE_REMINDER,
                        paymentDedupeRef,
                        t,
                        mail,
                        sms,
                        subject,
                        body,
                        smsText
                    );
                }
            }
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
