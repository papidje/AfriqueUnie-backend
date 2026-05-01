package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.GradingDtos.PeriodAverageBreakdown;
import friasoft.gn.schoolapp.dto.GradingDtos.StudentPeriodDashboardResponse;
import friasoft.gn.schoolapp.service.GradingService;
import friasoft.gn.schoolapp.service.document.StudentPeriodBulletinPdfService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.READ;

@RestController
@RequestMapping("/api/students")
@AllArgsConstructor
public class GradingController {

    private final GradingService gradingService;
    private final StudentPeriodBulletinPdfService studentPeriodBulletinPdfService;

    /**
     * Moyenne périodique : contrôle continu (interro / devoir / quiz) pondéré sur /20,
     * puis moyenne de période intégrant la composition avec le poids {@code application.grading.composition-weight-in-period} (défaut 0,5).
     */
    @PreAuthorize(READ)
    @GetMapping("/{studentId}/grading-periods/{periodId}/period-average")
    public ResponseEntity<PeriodAverageBreakdown> periodAverage(
        @PathVariable Long studentId,
        @PathVariable Long periodId
    ) {
        try {
            return ResponseEntity.ok(gradingService.computePeriodAverageForStudent(studentId, periodId));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                e.getMessage() != null && e.getMessage().contains("introuvable")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST,
                e.getMessage()
            );
        }
    }

    /**
     * Tableau de bord par matière (moyennes CC, composition, périodique) pour un élève et une période.
     */
    @PreAuthorize(READ)
    @GetMapping("/{studentId}/grading-periods/{periodId}/period-dashboard")
    public ResponseEntity<StudentPeriodDashboardResponse> periodDashboard(
        @PathVariable Long studentId,
        @PathVariable Long periodId
    ) {
        try {
            return ResponseEntity.ok(gradingService.buildStudentPeriodDashboard(studentId, periodId));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                e.getMessage() != null && e.getMessage().contains("introuvable")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST,
                e.getMessage()
            );
        }
    }

    /**
     * Bulletin individuel (PDF) pour l’élève sur la période : mêmes agrégats que le tableau de bord.
     */
    @PreAuthorize(READ)
    @GetMapping(value = "/{studentId}/grading-periods/{periodId}/bulletin-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> bulletinPdf(
        @PathVariable Long studentId,
        @PathVariable Long periodId
    ) {
        try {
            byte[] bytes = studentPeriodBulletinPdfService.buildPdf(studentId, periodId);
            String filename = "bulletin-eleve-" + studentId + "-periode-" + periodId + ".pdf";
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(new ByteArrayResource(bytes));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                e.getMessage() != null && e.getMessage().contains("introuvable")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST,
                e.getMessage()
            );
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
