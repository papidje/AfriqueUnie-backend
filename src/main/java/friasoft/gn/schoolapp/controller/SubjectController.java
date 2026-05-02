package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.school.Subject;
import friasoft.gn.schoolapp.service.SubjectService;
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
@RequestMapping("/api/subjects")
@AllArgsConstructor
public class SubjectController {

    private final SubjectService service;

    @PreAuthorize(READ)
    @GetMapping
    public List<Subject> list(@RequestParam Long schoolId) {
        return service.findCatalogForSchool(schoolId);
    }

    @PreAuthorize(READ)
    @GetMapping("/{id}")
    public ResponseEntity<Subject> getById(@PathVariable Long id, @RequestParam Long schoolId) {
        return service.findInCatalog(schoolId, id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize(WRITE)
    @PostMapping
    public ResponseEntity<Subject> create(@RequestParam Long schoolId, @RequestBody Subject subject) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.createForSchool(schoolId, subject));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize(WRITE)
    @PutMapping("/{id}")
    public ResponseEntity<Subject> update(
        @PathVariable Long id,
        @RequestParam Long schoolId,
        @RequestBody Subject subject
    ) {
        try {
            return ResponseEntity.ok(service.updateInCatalog(schoolId, id, subject));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize(WRITE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam Long schoolId) {
        try {
            service.deleteInCatalog(schoolId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }
}
