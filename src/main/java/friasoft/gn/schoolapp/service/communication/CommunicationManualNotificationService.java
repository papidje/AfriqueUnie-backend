package friasoft.gn.schoolapp.service.communication;

import friasoft.gn.schoolapp.dto.communication.CommunicationDtos.CommunicationManualSendRequest;
import friasoft.gn.schoolapp.dto.communication.CommunicationDtos.CommunicationManualSendResponse;
import friasoft.gn.schoolapp.entity.school.NotificationDeliveryHistory;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommunicationManualNotificationService {

    private static final int PAGE = 200;

    private final SchoolService schoolService;
    private final ISchoolClassRepository schoolClassRepository;
    private final IStudentRepository studentRepository;
    private final ParentDeliveryResolver parentDeliveryResolver;
    private final CommunicationMailDispatchService communicationMailDispatchService;
    private final MockSmsService mockSmsService;
    private final NotificationOutcomeRecorder notificationOutcomeRecorder;

    public CommunicationManualSendResponse dispatch(Long tenantId, CommunicationManualSendRequest req) {
        if (req.schoolId() == null || !StringUtils.hasText(req.title()) || !StringUtils.hasText(req.message())) {
            throw new IllegalArgumentException("schoolId, titre et message sont obligatoires.");
        }
        schoolService.assertCurrentUserCanAccessSchool(req.schoolId());

        List<Long> classIds = normalizeClassIds(req.schoolClassIds());
        validateClassSelection(req.schoolId(), classIds);

        boolean mail = channelAllowsMail(req.channel());
        boolean sms = channelAllowsSms(req.channel());
        if (!mail && !sms) {
            throw new IllegalArgumentException("Canal invalide (EMAIL, SMS ou BOTH).");
        }

        String subject = "[Important] " + req.title().trim();
        String smsBody = req.title().trim() + " — " + req.message().trim();
        String coreHtml = "<h2 style=\"font-size:18px\">" + escapeHtml(req.title()) + "</h2><p>"
            + nl2br(escapeHtml(req.message())) + "</p>";
        String archivePlain = manualArchivePlain(req.title().trim(), req.message().trim());

        Set<String> sentEmailsNorm = new HashSet<>();
        Set<String> sentPhonesNorm = new HashSet<>();

        int attempted = 0;
        int successes = 0;
        int failures = 0;
        int skippedDuplicates = 0;
        int pageIdx = 0;

        while (true) {
            Page<Student> page = fetchPage(req.schoolId(), classIds, pageIdx);
            if (page.isEmpty()) {
                break;
            }
            for (Student student : page.getContent()) {
                Student full = studentRepository.findByIdWithParentsAndClass(student.getId()).orElse(student);
                for (ParentDeliveryResolver.ParentDeliveryTarget t : parentDeliveryResolver.resolveDistinctEmailTargets(full)) {
                    if (mail && StringUtils.hasText(t.email())) {
                        String ek = normalizeEmail(t.email());
                        if (!sentEmailsNorm.add(ek)) {
                            skippedDuplicates++;
                        } else {
                            attempted++;
                            try {
                                String body = "<p>" + escapeHtml(t.greetingKey()) + "</p>" + coreHtml;
                                communicationMailDispatchService.sendHtml(t.email(), subject, body);
                                successes++;
                            } catch (Exception e) {
                                failures++;
                            }
                        }
                    }

                    if (sms && StringUtils.hasText(t.phone())) {
                        String pk = normalizePhone(t.phone());
                        if (pk.isEmpty()) {
                            continue;
                        }
                        if (!sentPhonesNorm.add(pk)) {
                            skippedDuplicates++;
                        } else {
                            attempted++;
                            try {
                                mockSmsService.sendSms(t.phone(), smsBody);
                                successes++;
                            } catch (Exception e) {
                                failures++;
                            }
                        }
                    }
                }
            }
            if (!page.hasNext()) {
                break;
            }
            pageIdx++;
        }

        String summary = attempted + " envoi(s) distinct(s), " + successes + " succès, " + failures + " échec(s)"
            + (skippedDuplicates > 0 ? ", " + skippedDuplicates + " ignoré(s) (doublon ou sans canal)" : "");
        if (failures > 0 && successes == 0) {
            notificationOutcomeRecorder.recordManualFailure(
                tenantId,
                req.schoolId(),
                CommunicationEventType.MANUAL_URGENT,
                mapChannel(mail, sms),
                summary,
                subject,
                previewPlain(req.message()),
                archivePlain,
                "Tous les envois ont échoué."
            );
        } else {
            notificationOutcomeRecorder.recordManualSuccess(
                tenantId,
                req.schoolId(),
                CommunicationEventType.MANUAL_URGENT,
                mapChannel(mail, sms),
                summary,
                subject,
                previewPlain(req.message()),
                archivePlain
            );
        }

        return new CommunicationManualSendResponse(attempted, successes, failures, skippedDuplicates);
    }

    private Page<Student> fetchPage(Long schoolId, List<Long> classIds, int pageIdx) {
        if (classIds.isEmpty()) {
            return studentRepository.findAllBySchoolIdAndActiveSchoolYear(schoolId, PageRequest.of(pageIdx, PAGE));
        }
        return studentRepository.findAllBySchoolIdAndClassIdsAndActiveSchoolYear(schoolId, classIds, PageRequest.of(pageIdx, PAGE));
    }

    private static List<Long> normalizeClassIds(List<Long> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream().filter(Objects::nonNull).distinct().sorted().toList();
    }

    private void validateClassSelection(Long schoolId, List<Long> classIds) {
        if (classIds.isEmpty()) {
            return;
        }
        long n = schoolClassRepository.countByIdsAndSchoolActiveYear(schoolId, classIds);
        if (n != classIds.size()) {
            throw new IllegalArgumentException(
                "Une ou plusieurs classes ne sont pas dans l’établissement ou dans l’année scolaire active."
            );
        }
    }

    private static String manualArchivePlain(String title, String message) {
        return "Titre : " + title + "\n\n" + message;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("\\D+", "");
    }

    private static NotificationDeliveryHistory.Channel mapChannel(boolean mail, boolean sms) {
        if (mail && sms) {
            return NotificationDeliveryHistory.Channel.BOTH;
        }
        if (sms) {
            return NotificationDeliveryHistory.Channel.SMS;
        }
        return NotificationDeliveryHistory.Channel.EMAIL;
    }

    private static boolean channelAllowsMail(String channel) {
        if (channel == null) {
            return true;
        }
        String c = channel.trim().toUpperCase(Locale.ROOT);
        return "EMAIL".equals(c) || "BOTH".equals(c);
    }

    private static boolean channelAllowsSms(String channel) {
        if (channel == null) {
            return false;
        }
        String c = channel.trim().toUpperCase(Locale.ROOT);
        return "SMS".equals(c) || "BOTH".equals(c);
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String nl2br(String s) {
        return s.replace("\n", "<br/>");
    }

    private static String previewPlain(String m) {
        if (m == null) {
            return "";
        }
        return m.length() > 400 ? m.substring(0, 400) + "…" : m;
    }
}
