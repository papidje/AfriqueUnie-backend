package friasoft.gn.schoolapp.service.communication;

import friasoft.gn.schoolapp.entity.school.ClassTimetableSlot;
import friasoft.gn.schoolapp.entity.school.NotificationBatchSettings;
import friasoft.gn.schoolapp.entity.school.NotificationTimetableState;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.IClassTimetableSlotRepository;
import friasoft.gn.schoolapp.repository.INotificationTimetableStateRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTimetableChangeJob {

    private final ISchoolClassRepository schoolClassRepository;
    private final IClassTimetableSlotRepository classTimetableSlotRepository;
    private final INotificationTimetableStateRepository notificationTimetableStateRepository;
    private final IStudentRepository studentRepository;
    private final ParentDeliveryResolver parentDeliveryResolver;
    private final NotificationDispatchAssistant notificationDispatchAssistant;

    @Transactional
    public void run(NotificationBatchSettings settings) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null || !Boolean.TRUE.equals(settings.getTimetableChangeEnabled())) {
            return;
        }
        boolean mail = Boolean.TRUE.equals(settings.getEmailEnabled());
        boolean sms = Boolean.TRUE.equals(settings.getSmsEnabled());
        if (!mail && !sms) {
            return;
        }

        String template = """
                <p>%GREETING%</p>
                <p>L’emploi du temps de la classe de <strong>%FN% %LN%</strong> a été mis à jour cette semaine.</p>
                <p>Consultez l’application ou contactez l’établissement pour le détail.</p>
                """;
        String subject = "Modification de l’emploi du temps";

        for (Long classId : schoolClassRepository.findIdsForActiveSchoolYears()) {
            Long schoolId = schoolClassRepository.findByIdWithYearAndSchool(classId)
                .map(sc -> sc.getYear().getSchool().getId())
                .orElse(null);
            if (schoolId == null) {
                log.debug("Batch EDT : impossible de résoudre l’école pour la classe {}", classId);
                continue;
            }
            List<ClassTimetableSlot> slots = classTimetableSlotRepository.findBySchoolClassIdWithSubject(classId);
            String fp = fingerprint(slots);
            Optional<NotificationTimetableState> opt = notificationTimetableStateRepository.findByTenantIdAndSchoolClass_Id(
                tenantId,
                classId
            );
            SchoolClass clazz = schoolClassRepository.getReferenceById(classId);

            if (opt.isEmpty()) {
                NotificationTimetableState created = NotificationTimetableState.builder()
                    .tenantId(tenantId)
                    .schoolClass(clazz)
                    .fingerprint(fp)
                    .changeSeq(0L)
                    .build();
                notificationTimetableStateRepository.save(created);
                continue;
            }

            NotificationTimetableState state = opt.get();
            if (fp.equals(state.getFingerprint())) {
                continue;
            }

            long seq = state.getChangeSeq() + 1;
            state.setFingerprint(fp);
            state.setChangeSeq(seq);
            notificationTimetableStateRepository.save(state);

            long dedupeRef = NotificationDedupeRefs.timetableChange(classId, seq);

            for (Student student : studentRepository.findBySchoolClassIdWithParents(classId)) {
                String smsText = "Emploi du temps modifié pour la classe de " + student.getFirstName() + " "
                    + student.getLastName() + ".";
                for (ParentDeliveryResolver.ParentDeliveryTarget t : parentDeliveryResolver.resolveDistinctEmailTargets(student)) {
                    String body = template
                        .replace("%GREETING%", escape(t.greetingKey()))
                        .replace("%FN%", escape(student.getFirstName()))
                        .replace("%LN%", escape(student.getLastName()));
                    notificationDispatchAssistant.sendParentTargetIfNew(
                        tenantId,
                        schoolId,
                        CommunicationEventType.TIMETABLE_CHANGED,
                        dedupeRef,
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

    private static String fingerprint(List<ClassTimetableSlot> slots) {
        String raw = slots.stream()
            .sorted(
                Comparator.comparing(ClassTimetableSlot::getDayOfWeek).thenComparing(ClassTimetableSlot::getSlotIndex)
            )
            .map(s -> s.getDayOfWeek() + ":" + s.getSlotIndex() + ":" + s.getClassSubject().getId())
            .collect(Collectors.joining("|"));
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (Exception e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
