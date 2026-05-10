package friasoft.gn.schoolapp.service.communication;

import friasoft.gn.schoolapp.dto.StudentPaymentStatusDTO;
import friasoft.gn.schoolapp.dto.communication.CommunicationDtos.CommunicationBatchSettingsResponse;
import friasoft.gn.schoolapp.dto.communication.CommunicationDtos.CommunicationBatchSettingsUpdateRequest;
import friasoft.gn.schoolapp.dto.communication.CommunicationDtos.CommunicationScheduledPreviewRow;
import friasoft.gn.schoolapp.entity.school.NotificationBatchSettings;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.IEvaluationRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.service.FinanceService;
import friasoft.gn.schoolapp.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunicationPreviewService {

    private final NotificationBatchSettingsService notificationBatchSettingsService;
    private final IEvaluationRepository evaluationRepository;
    private final ISchoolClassRepository schoolClassRepository;
    private final FinanceService financeService;
    private final IStudentRepository studentRepository;
    private final ParentDeliveryResolver parentDeliveryResolver;

    @Transactional(readOnly = true)
    public List<CommunicationScheduledPreviewRow> previewScheduledForSchool(Long schoolId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant manquant.");
        }
        NotificationBatchSettings s = notificationBatchSettingsService.resolveForTenant(tenantId);
        List<CommunicationScheduledPreviewRow> rows = new ArrayList<>();

        int days = Math.max(1, s.getEvaluationReminderDaysBefore());
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime toExcl = LocalDate.now().plusDays(days + 1L).atStartOfDay();
        long evalCount = Boolean.TRUE.equals(s.getEvaluationReminderEnabled())
            ? evaluationRepository
                .findIdsStartingBetweenForActiveYearAndSchool(schoolId, from, toExcl, PageRequest.of(0, 1))
                .getTotalElements()
            : 0L;
        rows.add(
            new CommunicationScheduledPreviewRow(
                "EVALUATION_REMINDER",
                "Rappels d’évaluations à venir",
                evalCount <= 0 ? 0L : evalCount * 2L,
                "Évaluations dont la date de début est dans les " + days + " prochains jours pour cet établissement (estimation ~2 parents par évaluation)."
            )
        );

        rows.add(
            new CommunicationScheduledPreviewRow(
                "PAYMENT_OVERDUE_REMINDER",
                "Relances paiement (soldes non soldés)",
                Boolean.TRUE.equals(s.getPaymentReminderEnabled()) ? estimateDebtRecipientsForSchool(schoolId) : 0L,
                "Parents distincts pour élèves avec solde attendu et paiement incomplet (classes de l’année active — cet établissement)."
            )
        );

        long classes = schoolClassRepository.findIdsForActiveSchoolYearBySchoolId(schoolId).size();
        rows.add(
            new CommunicationScheduledPreviewRow(
                "TIMETABLE_CHANGED",
                "Surveillance emploi du temps",
                Boolean.TRUE.equals(s.getTimetableChangeEnabled()) ? classes : 0,
                "Nombre de classes suivies pour cet établissement ; envoi seulement si la grille change depuis le dernier cycle."
            )
        );

        return rows;
    }

    private long estimateDebtRecipientsForSchool(Long schoolId) {
        long total = 0;
        for (Long classId : schoolClassRepository.findIdsForActiveSchoolYearBySchoolId(schoolId)) {
            List<StudentPaymentStatusDTO> rows;
            try {
                rows = financeService.getClassPaymentStatusTrusted(classId);
            } catch (Exception ex) {
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
                Student student = studentRepository.findByIdWithParentsAndClass(row.studentId()).orElse(null);
                if (student == null) {
                    continue;
                }
                total += parentDeliveryResolver.resolveDistinctEmailTargets(student).size();
            }
        }
        return total;
    }

    public static CommunicationBatchSettingsResponse toResponse(NotificationBatchSettings s) {
        return new CommunicationBatchSettingsResponse(
            s.getEvaluationReminderDaysBefore(),
            Boolean.TRUE.equals(s.getEvaluationReminderEnabled()),
            Boolean.TRUE.equals(s.getPaymentReminderEnabled()),
            Boolean.TRUE.equals(s.getTimetableChangeEnabled()),
            s.getBatchChunkSize(),
            Boolean.TRUE.equals(s.getEmailEnabled()),
            Boolean.TRUE.equals(s.getSmsEnabled())
        );
    }

    public static void applyUpdate(NotificationBatchSettings target, CommunicationBatchSettingsUpdateRequest req) {
        if (req.evaluationReminderDaysBefore() != null) {
            target.setEvaluationReminderDaysBefore(Math.max(1, Math.min(req.evaluationReminderDaysBefore(), 30)));
        }
        if (req.evaluationReminderEnabled() != null) {
            target.setEvaluationReminderEnabled(req.evaluationReminderEnabled());
        }
        if (req.paymentReminderEnabled() != null) {
            target.setPaymentReminderEnabled(req.paymentReminderEnabled());
        }
        if (req.timetableChangeEnabled() != null) {
            target.setTimetableChangeEnabled(req.timetableChangeEnabled());
        }
        if (req.batchChunkSize() != null) {
            target.setBatchChunkSize(Math.max(10, Math.min(req.batchChunkSize(), 500)));
        }
        if (req.emailEnabled() != null) {
            target.setEmailEnabled(req.emailEnabled());
        }
        if (req.smsEnabled() != null) {
            target.setSmsEnabled(req.smsEnabled());
        }
    }
}
