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
    public List<Subject> list() {
        return service.findAll();
    }

    @PreAuthorize(READ)
    @GetMapping("/{id}")
    public ResponseEntity<Subject> getById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize(WRITE)
    @PostMapping
    public ResponseEntity<Subject> create(@RequestBody Subject subject) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.save(subject));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize(WRITE)
    @PutMapping("/{id}")
    public ResponseEntity<Subject> update(@PathVariable Long id, @RequestBody Subject subject) {
        return service.findById(id)
            .map(existing -> {
                subject.setId(id);
                try {
                    return ResponseEntity.ok(service.save(subject));
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize(WRITE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }
}
