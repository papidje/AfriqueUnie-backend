package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.EvaluationDtos.CreateEvaluationRequest;
import friasoft.gn.schoolapp.dto.EvaluationDtos.EvaluationResponse;
import friasoft.gn.schoolapp.dto.EvaluationDtos.GradeSheetResponse;
import friasoft.gn.schoolapp.dto.EvaluationDtos.GradeUpsertRequest;
import friasoft.gn.schoolapp.dto.EvaluationDtos.GradingPeriodSummary;
import friasoft.gn.schoolapp.service.EvaluationService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.READ;
import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.WRITE_OR_TEACHER;

@RestController
@AllArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PreAuthorize(READ)
    @GetMapping("/api/school-classes/{classId}/grading-periods")
    public List<GradingPeriodSummary> listGradingPeriods(@PathVariable Long classId) {
        try {
            return evaluationService.listGradingPeriodsForClass(classId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PreAuthorize(READ)
    @GetMapping("/api/school-classes/{classId}/evaluations")
    public List<EvaluationResponse> listForClass(@PathVariable Long classId) {
        try {
            return evaluationService.listForClass(classId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PreAuthorize(WRITE_OR_TEACHER)
    @PostMapping("/api/school-classes/{classId}/evaluations")
    public ResponseEntity<EvaluationResponse> create(
        @PathVariable Long classId,
        @RequestBody CreateEvaluationRequest body
    ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(evaluationService.create(classId, body));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize(READ)
    @GetMapping("/api/evaluations/{evaluationId}")
    public EvaluationResponse getById(@PathVariable Long evaluationId) {
        try {
            return evaluationService.getById(evaluationId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PreAuthorize(WRITE_OR_TEACHER)
    @PutMapping("/api/evaluations/{evaluationId}")
    public EvaluationResponse update(
        @PathVariable Long evaluationId,
        @RequestBody CreateEvaluationRequest body
    ) {
        try {
            return evaluationService.update(evaluationId, body);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize(WRITE_OR_TEACHER)
    @DeleteMapping("/api/evaluations/{evaluationId}")
    public ResponseEntity<Void> delete(@PathVariable Long evaluationId) {
        try {
            evaluationService.delete(evaluationId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PreAuthorize(READ)
    @GetMapping("/api/evaluations/{evaluationId}/grade-sheet")
    public GradeSheetResponse gradeSheet(@PathVariable Long evaluationId) {
        try {
            return evaluationService.getGradeSheet(evaluationId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PreAuthorize(WRITE_OR_TEACHER)
    @PutMapping("/api/evaluations/{evaluationId}/grades")
    public ResponseEntity<Void> saveGrades(
        @PathVariable Long evaluationId,
        @RequestBody List<GradeUpsertRequest> body
    ) {
        try {
            evaluationService.saveGrades(evaluationId, body);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
