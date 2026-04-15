package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.ClassSubjectDtos.AssignClassSubjectTeacherRequest;
import friasoft.gn.schoolapp.dto.ClassSubjectDtos.ClassPlanningView;
import friasoft.gn.schoolapp.dto.ClassSubjectDtos.ClassSubjectResponse;
import friasoft.gn.schoolapp.dto.ClassSubjectDtos.CreateClassSubjectRequest;
import friasoft.gn.schoolapp.dto.ClassSubjectDtos.UpdateClassSubjectCoefficientRequest;
import friasoft.gn.schoolapp.service.ClassSubjectService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@AllArgsConstructor
public class ClassSubjectController {

    private final ClassSubjectService service;

    @GetMapping("/api/school-classes/{classId}/class-subjects")
    public List<ClassSubjectResponse> listForClass(@PathVariable Long classId) {
        try {
            return service.listForClass(classId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/api/school-classes/{classId}/planning")
    public ClassPlanningView getPlanning(@PathVariable Long classId) {
        try {
            return service.getPlanning(classId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/api/school-classes/{classId}/class-subjects")
    public ResponseEntity<ClassSubjectResponse> create(
        @PathVariable Long classId,
        @RequestBody CreateClassSubjectRequest body
    ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(classId, body));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/api/class-subjects/{id}/coefficient")
    public ClassSubjectResponse updateCoefficient(
        @PathVariable Long id,
        @RequestBody UpdateClassSubjectCoefficientRequest body
    ) {
        try {
            return service.updateCoefficient(id, body.coefficient());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/api/class-subjects/{id}/teacher")
    public ClassSubjectResponse assignTeacher(
        @PathVariable Long id,
        @RequestBody AssignClassSubjectTeacherRequest body
    ) {
        try {
            return service.assignTeacher(id, body.teacherId());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/api/class-subjects/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
