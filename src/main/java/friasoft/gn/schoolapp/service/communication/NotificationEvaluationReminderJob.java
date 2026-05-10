package friasoft.gn.schoolapp.service.communication;

import friasoft.gn.schoolapp.entity.school.Evaluation;
import friasoft.gn.schoolapp.entity.school.NotificationBatchSettings;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.IEvaluationRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEvaluationReminderJob {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final IEvaluationRepository evaluationRepository;
    private final IStudentRepository studentRepository;
    private final ParentDeliveryResolver parentDeliveryResolver;
    private final NotificationDispatchAssistant notificationDispatchAssistant;

    @Transactional(readOnly = true)
    public void run(NotificationBatchSettings settings) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null || !Boolean.TRUE.equals(settings.getEvaluationReminderEnabled())) {
            return;
        }
        boolean mail = Boolean.TRUE.equals(settings.getEmailEnabled());
        boolean sms = Boolean.TRUE.equals(settings.getSmsEnabled());
        if (!mail && !sms) {
            return;
        }
        int days = Math.max(1, settings.getEvaluationReminderDaysBefore());
        int chunk = Math.max(10, settings.getBatchChunkSize());
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime toExclusive = LocalDate.now().plusDays(days + 1L).atStartOfDay();

        Pageable pageable = PageRequest.of(0, chunk);
        Page<Long> page;
        do {
            page = evaluationRepository.findIdsStartingBetweenForActiveYear(from, toExclusive, pageable);
            for (Long evalId : page.getContent()) {
                evaluationRepository.findByIdWithDetails(evalId).ifPresent(ev -> processEvaluation(ev, tenantId, mail, sms));
            }
            pageable = page.nextPageable();
        } while (page.hasNext());
    }

    private void processEvaluation(Evaluation e, Long tenantId, boolean mail, boolean sms) {
        Long schoolId = e.getClassSubject().getSchoolClass().getYear().getSchool().getId();
        long classId = e.getClassSubject().getSchoolClass().getId();
        String subjectName = e.getClassSubject().getSubject().getName();
        String subject = "Rappel : évaluation — " + subjectName;
        String when = DF.format(e.getStartDate());
        for (Student student : studentRepository.findBySchoolClassIdWithParents(classId)) {
        String template = """
                <p>%GREETING%</p>
                <p>Une évaluation est prévue pour <strong>%FN% %LN%</strong> : <strong>%TITLE%</strong>.</p>
                <p>Période : %PERIOD%<br/>Début : %START%<br/>Fin : %END%</p>
                <p>Cordialement,<br/>Votre établissement scolaire</p>
                """;
            String smsText = "Rappel évaluation " + subjectName + " (" + when + ") pour " + student.getFirstName() + " "
                + student.getLastName() + ".";
            for (ParentDeliveryResolver.ParentDeliveryTarget t : parentDeliveryResolver.resolveDistinctEmailTargets(student)) {
                String personalised = template
                    .replace("%GREETING%", escapeHtml(t.greetingKey()))
                    .replace("%FN%", escapeHtml(student.getFirstName()))
                    .replace("%LN%", escapeHtml(student.getLastName()))
                    .replace("%TITLE%", escapeHtml(e.getTitle()))
                    .replace("%PERIOD%", escapeHtml(e.getGradingPeriod().getName()))
                    .replace("%START%", DF.format(e.getStartDate()))
                    .replace("%END%", DF.format(e.getEndDate()));
                notificationDispatchAssistant.sendParentTargetIfNew(
                    tenantId,
                    schoolId,
                    CommunicationEventType.EVALUATION_REMINDER,
                    e.getId(),
                    t,
                    mail,
                    sms,
                    subject,
                    personalised,
                    smsText
                );
            }
        }
    }

    private static String escapeHtml(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
