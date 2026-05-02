package friasoft.gn.schoolapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import friasoft.gn.schoolapp.dto.GradingDtos.PeriodNotesGridResponse;
import friasoft.gn.schoolapp.dto.GradingDtos.PeriodNotesGridRow;
import friasoft.gn.schoolapp.dto.GradingDtos.StudentPeriodBuildData;
import friasoft.gn.schoolapp.entity.school.GradingPeriod;
import friasoft.gn.schoolapp.entity.school.StudentGradingSnapshot;
import friasoft.gn.schoolapp.repository.IGradingPeriodRepository;
import friasoft.gn.schoolapp.repository.IStudentGradingSnapshotRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Transaction unitaire de recalcul des snapshots (une classe, une période).
 */
@Service
@RequiredArgsConstructor
public class GradingSnapshotRecomputeService {

    private final GradingService gradingService;
    private final IStudentGradingSnapshotRepository snapshotRepository;
    private final IGradingPeriodRepository gradingPeriodRepository;
    private final IStudentRepository studentRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recomputeForClassAndPeriod(long classId, long periodId) {
        GradingPeriod period = gradingPeriodRepository
            .findByIdWithClassAndSchool(periodId)
            .orElseThrow(() -> new IllegalArgumentException("Période introuvable."));
        if (!Objects.equals(period.getSchoolClass().getId(), classId)) {
            throw new IllegalArgumentException("La période ne correspond pas à cette classe.");
        }
        Long tenantId = period.getSchoolClass().getTenantId();
        TenantContext.setTenantId(tenantId);
        try {
            snapshotRepository.deleteByClassAndPeriod(classId, periodId);
            PeriodNotesGridResponse grid = gradingService.buildPeriodNotesGridLiveOnly(classId, periodId);
            Map<Long, Integer> rankBy = computeRanks(grid);
            Instant now = Instant.now();
            for (PeriodNotesGridRow row : grid.rows()) {
                StudentPeriodBuildData data = gradingService.buildStudentPeriodBuildData(row.studentId(), classId, periodId);
                String json = objectMapper.writeValueAsString(data.subjects());
                StudentGradingSnapshot snap = new StudentGradingSnapshot();
                snap.setTenantId(tenantId);
                snap.setStudent(studentRepository.getReferenceById(row.studentId()));
                snap.setSchoolClass(period.getSchoolClass());
                snap.setGradingPeriod(period);
                snap.setGradingPeriodName(period.getName());
                snap.setSubjectAveragesJson(json);
                snap.setPeriodGeneralAverage(data.generalAverage());
                snap.setRankInClass(rankBy.get(row.studentId()));
                snap.setTotalEvaluations(data.evaluatedEvaluationsCount());
                snap.setCompositionWeight(grid.compositionWeight());
                snap.setLastUpdatedAt(now);
                snapshotRepository.save(snap);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Échec snapshot classe " + classId + " période " + periodId, e);
        } finally {
            TenantContext.clear();
        }
    }

    private static Map<Long, Integer> computeRanks(PeriodNotesGridResponse grid) {
        Map<Long, Integer> m = new HashMap<>();
        for (PeriodNotesGridRow r : grid.rows()) {
            if (r.generalAverage() == null) {
                m.put(r.studentId(), null);
                continue;
            }
            double myG = r.generalAverage();
            int better = 0;
            for (PeriodNotesGridRow o : grid.rows()) {
                if (o.generalAverage() != null && o.generalAverage() > myG + 1e-6) {
                    better++;
                }
            }
            m.put(r.studentId(), better + 1);
        }
        return m;
    }
}
