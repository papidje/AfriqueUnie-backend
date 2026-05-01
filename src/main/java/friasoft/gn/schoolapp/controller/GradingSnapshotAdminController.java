package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.service.GradingBatchService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.WRITE;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class GradingSnapshotAdminController {

    private final GradingBatchService gradingBatchService;

    /**
     * Recalcul manuel des snapshots pour une classe et une période (mêmes données que le job nocturne).
     */
    @PreAuthorize(WRITE)
    @PostMapping("/school-classes/{classId}/grading-periods/{periodId}/snapshots/recompute")
    public ResponseEntity<Void> recomputeClassPeriod(
        @PathVariable Long classId,
        @PathVariable Long periodId
    ) {
        gradingBatchService.recalculateSnapshotsForClassAndPeriod(classId, periodId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Recalcul complet (toutes les classes / toutes les périodes) — réservé aux opérations admin lourdes.
     */
    @PreAuthorize(WRITE)
    @PostMapping("/grading-snapshots/recalculate-all")
    public ResponseEntity<Void> recomputeAll() {
        gradingBatchService.recalculateAllSnapshots();
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
