package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.GradingPeriod;
import friasoft.gn.schoolapp.repository.IGradingPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Planification du recalcul des snapshots (délègue chaque (classe, période) en transaction).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradingBatchService {

    private final GradingSnapshotRecomputeService recomputeService;
    private final IGradingPeriodRepository gradingPeriodRepository;

    /** Lundi–vendredi, 03:00 (heure du serveur). */
    @Scheduled(cron = "0 0 3 * * MON-FRI")
    public void scheduledRecalculate() {
        log.info("Début recalcul nocturne des snapshots de notes (moyennes / rangs)");
        try {
            recalculateAllSnapshots();
        } catch (Exception e) {
            log.error("Recalcul snapshots interrompu: {}", e.getMessage(), e);
        }
        log.info("Fin recalcul nocturne des snapshots de notes");
    }

    public void recalculateAllSnapshots() {
        List<GradingPeriod> periods = gradingPeriodRepository.findAllWithClassYearAndSchool();
        for (GradingPeriod gp : periods) {
            try {
                recomputeService.recomputeForClassAndPeriod(gp.getSchoolClass().getId(), gp.getId());
            } catch (Exception e) {
                log.warn(
                    "Snapshot ignoré classe {} période {}: {}",
                    gp.getSchoolClass().getId(),
                    gp.getId(),
                    e.getMessage()
                );
            }
        }
    }

    public void recalculateSnapshotsForClassAndPeriod(long classId, long periodId) {
        recomputeService.recomputeForClassAndPeriod(classId, periodId);
    }
}
