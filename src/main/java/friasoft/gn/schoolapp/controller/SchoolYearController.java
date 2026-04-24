package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.school.SchoolYear;
import friasoft.gn.schoolapp.service.SchoolService;
import friasoft.gn.schoolapp.service.SchoolYearService;
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
@RequestMapping("/api/school-years")
@AllArgsConstructor
public class SchoolYearController {

    private final SchoolYearService service;
    private final SchoolService schoolService;

    @PreAuthorize(READ)
    @GetMapping("/{id}")
    public ResponseEntity<SchoolYear> getById(@PathVariable Long id) {
        return service.findById(id)
            .map(y -> {
                schoolService.assertCurrentUserCanAccessSchool(y.getSchool().getId());
                return ResponseEntity.ok(y);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize(READ)
    @GetMapping("/school/{schoolId}")
    public List<SchoolYear> getBySchool(@PathVariable Long schoolId) {
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        return service.findBySchool(schoolId);
    }

    /** Année scolaire active pour l’établissement (après contrôle d’accès tenant). */
    @PreAuthorize(READ)
    @GetMapping("/school/{schoolId}/active")
    public ResponseEntity<SchoolYear> getActiveForSchool(@PathVariable Long schoolId) {
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        return service.getActiveYearForSchool(schoolId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize(WRITE)
    @PostMapping
    public ResponseEntity<SchoolYear> create(@RequestBody SchoolYear year) {
        if (year.getSchool() == null || year.getSchool().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "school.id requis");
        }
        schoolService.assertCurrentUserCanAccessSchool(year.getSchool().getId());
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.save(year));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize(WRITE)
    @PutMapping("/{id}")
    public ResponseEntity<SchoolYear> update(@PathVariable Long id, @RequestBody SchoolYear year) {
        return service.findById(id)
            .map(existing -> {
                schoolService.assertCurrentUserCanAccessSchool(existing.getSchool().getId());
                year.setId(id);
                if (year.getSchool() == null || year.getSchool().getId() == null) {
                    year.setSchool(existing.getSchool());
                } else {
                    schoolService.assertCurrentUserCanAccessSchool(year.getSchool().getId());
                }
                try {
                    return ResponseEntity.ok(service.save(year));
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize(WRITE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.findById(id)
            .<ResponseEntity<Void>>map(y -> {
                schoolService.assertCurrentUserCanAccessSchool(y.getSchool().getId());
                service.deleteById(id);
                return ResponseEntity.noContent().build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
