package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.GradingDtos.PeriodNotesGridResponse;
import friasoft.gn.schoolapp.dto.EvaluationDtos.UpdateClassPeriodTypeRequest;
import friasoft.gn.schoolapp.dto.EvaluationDtos.UpdateGradingPeriodsScheduleRequest;
import friasoft.gn.schoolapp.dto.response.SchoolClassOverviewResponse;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.service.GradingPeriodSettingsService;
import friasoft.gn.schoolapp.service.GradingService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import friasoft.gn.schoolapp.service.SchoolClassService;
import friasoft.gn.schoolapp.service.document.ClassPeriodGradesPdfService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.READ;
import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.WRITE;

@RestController
@RequestMapping("/api/school-classes")
@AllArgsConstructor
public class SchoolClassController {

    private final SchoolClassService service;
    private final GradingService gradingService;
    private final ClassPeriodGradesPdfService classPeriodGradesPdfService;
    private final GradingPeriodSettingsService gradingPeriodSettingsService;

    @PreAuthorize(READ)
    @GetMapping("/{id}")
    public ResponseEntity<SchoolClass> getById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize(READ)
    @GetMapping("/year/{yearId}")
    public List<SchoolClass> getByYear(@PathVariable Long yearId) {
        return service.findByYear(yearId);
    }

    /**
     * Liste des classes pour l’année scolaire <strong>active</strong> de l’établissement
     * (ex. école sélectionnée côté Angular {@code sessionStorage.activeSchoolId}).
     */
    @PreAuthorize(READ)
    @GetMapping("/school/{schoolId}/active-year")
    public List<SchoolClass> listForActiveSchoolYear(@PathVariable Long schoolId) {
        return service.listForActiveSchoolYear(schoolId);
    }

    @PreAuthorize(READ)
    @GetMapping("/school/{schoolId}/active-year/overview")
    public List<SchoolClassOverviewResponse> listOverviewForActiveSchoolYear(@PathVariable Long schoolId) {
        return service.listOverviewForActiveSchoolYear(schoolId);
    }

    /**
     * Grille des moyennes de période par matière (lignes = élèves, colonnes = matières de la classe),
     * plus moyenne générale pondérée sur la période.
     */
    @PreAuthorize(READ)
    @GetMapping("/{classId}/grading-periods/{periodId}/notes-grid")
    public ResponseEntity<PeriodNotesGridResponse> periodNotesGrid(
        @PathVariable Long classId,
        @PathVariable Long periodId
    ) {
        try {
            return ResponseEntity.ok(gradingService.buildPeriodNotesGrid(classId, periodId));
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
     * Relevé de notes (PDF) pour la classe et la période : mêmes données que la grille API.
     */
    @PreAuthorize(READ)
    @GetMapping(value = "/{classId}/grading-periods/{periodId}/notes-grid/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> periodNotesGridPdf(
        @PathVariable Long classId,
        @PathVariable Long periodId
    ) {
        try {
            byte[] bytes = classPeriodGradesPdfService.buildPdf(classId, periodId);
            String filename = "releve-notes-classe-" + classId + "-periode-" + periodId + ".pdf";
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
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

    @PreAuthorize(WRITE)
    @PutMapping("/{classId}/period-type")
    public ResponseEntity<Void> updatePeriodType(
        @PathVariable Long classId,
        @RequestBody UpdateClassPeriodTypeRequest body
    ) {
        try {
            gradingPeriodSettingsService.updatePeriodType(classId, body.periodType());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                e.getMessage() != null && e.getMessage().contains("introuvable")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST,
                e.getMessage()
            );
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PreAuthorize(WRITE)
    @PutMapping("/{classId}/grading-periods/schedule")
    public ResponseEntity<Void> updateGradingPeriodsSchedule(
        @PathVariable Long classId,
        @RequestBody UpdateGradingPeriodsScheduleRequest body
    ) {
        try {
            gradingPeriodSettingsService.updateGradingPeriodsSchedule(classId, body);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                e.getMessage() != null && e.getMessage().contains("introuvable")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST,
                e.getMessage()
            );
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PreAuthorize(WRITE)
    @PostMapping
    public ResponseEntity<SchoolClass> create(@RequestBody SchoolClass schoolClass) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.save(schoolClass));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }
}
