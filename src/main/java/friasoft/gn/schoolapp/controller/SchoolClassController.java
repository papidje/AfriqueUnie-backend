package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.response.SchoolClassOverviewResponse;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.service.SchoolClassService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/school-classes")
@AllArgsConstructor
public class SchoolClassController {

    private final SchoolClassService service;

    @GetMapping("/{id}")
    public ResponseEntity<SchoolClass> getById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/year/{yearId}")
    public List<SchoolClass> getByYear(@PathVariable Long yearId) {
        return service.findByYear(yearId);
    }

    /**
     * Liste des classes pour l’année scolaire <strong>active</strong> de l’établissement
     * (ex. école sélectionnée côté Angular {@code sessionStorage.activeSchoolId}).
     */
    @GetMapping("/school/{schoolId}/active-year")
    public List<SchoolClass> listForActiveSchoolYear(@PathVariable Long schoolId) {
        return service.listForActiveSchoolYear(schoolId);
    }

    @GetMapping("/school/{schoolId}/active-year/overview")
    public List<SchoolClassOverviewResponse> listOverviewForActiveSchoolYear(@PathVariable Long schoolId) {
        return service.listOverviewForActiveSchoolYear(schoolId);
    }

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
