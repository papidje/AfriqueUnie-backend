package friasoft.gn.schoolapp.service.communication;

import friasoft.gn.schoolapp.entity.school.NotificationDeliveryHistory;
import friasoft.gn.schoolapp.repository.INotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NotificationDispatchAssistant {

    private final CommunicationMailDispatchService mailDispatchService;
    private final MockSmsService mockSmsService;
    private final INotificationLogRepository notificationLogRepository;
    private final NotificationOutcomeRecorder notificationOutcomeRecorder;

    public void sendParentTargetIfNew(
        Long tenantId,
        Long schoolId,
        String eventType,
        Long referenceId,
        ParentDeliveryResolver.ParentDeliveryTarget target,
        boolean emailEnabled,
        boolean smsEnabled,
        String mailSubject,
        String htmlBody,
        String smsFallbackText
    ) {
        long pid = target.parentDedupeId();
        if (notificationLogRepository.existsBySchoolIdAndEventTypeAndReferenceIdAndParentId(
            schoolId, eventType, referenceId, pid
        )) {
            return;
        }
        NotificationDeliveryHistory.Channel channel = resolveChannel(emailEnabled, smsEnabled, target);
        String summary = target.email();
        try {
            if (emailEnabled && StringUtils.hasText(target.email())) {
                mailDispatchService.sendHtml(target.email(), mailSubject, htmlBody);
            }
            if (smsEnabled && StringUtils.hasText(target.phone())) {
                String sms = StringUtils.hasText(smsFallbackText) ? smsFallbackText : toPlain(htmlBody);
                mockSmsService.sendSms(target.phone(), sms);
            }
            notificationOutcomeRecorder.recordSuccess(
                tenantId,
                schoolId,
                eventType,
                referenceId,
                pid,
                channel,
                summary,
                mailSubject,
                preview(htmlBody),
                fullPlain(htmlBody)
            );
        } catch (Exception e) {
            notificationOutcomeRecorder.recordFailure(
                tenantId,
                schoolId,
                eventType,
                referenceId,
                pid,
                channel,
                summary,
                mailSubject,
                preview(htmlBody),
                fullPlain(htmlBody),
                e.getMessage()
            );
        }
    }

    private static NotificationDeliveryHistory.Channel resolveChannel(
        boolean emailEnabled,
        boolean smsEnabled,
        ParentDeliveryResolver.ParentDeliveryTarget target
    ) {
        boolean hasMail = emailEnabled && StringUtils.hasText(target.email());
        boolean hasSms = smsEnabled && StringUtils.hasText(target.phone());
        if (hasMail && hasSms) {
            return NotificationDeliveryHistory.Channel.BOTH;
        }
        if (hasSms) {
            return NotificationDeliveryHistory.Channel.SMS;
        }
        return NotificationDeliveryHistory.Channel.EMAIL;
    }

    private static String preview(String html) {
        if (html == null) {
            return null;
        }
        String p = toPlain(html);
        return p.length() > 400 ? p.substring(0, 400) + "…" : p;
    }

    private static String fullPlain(String html) {
        if (html == null) {
            return null;
        }
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private static String toPlain(String html) {
        return fullPlain(html);
    }
}
