package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.school.SchoolYear;
import friasoft.gn.schoolapp.service.SchoolYearService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/school-years")
@AllArgsConstructor
public class SchoolYearController {

    private SchoolYearService service;

    @GetMapping("/{id}")
    public ResponseEntity<SchoolYear> getById(@PathVariable Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/school/{schoolId}")
    public List<SchoolYear> getBySchool(@PathVariable Long schoolId) {
        return service.findBySchool(schoolId);
    }

    @PostMapping
    public SchoolYear create(@RequestBody SchoolYear year) {
        return service.save(year);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SchoolYear> update(@PathVariable Long id, @RequestBody SchoolYear year) {
        return service.findById(id)
            .map(existing -> {
                year.setId(id);
                return ResponseEntity.ok(service.save(year));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.findById(id).ifPresent(y -> service.save(y)); // ou service.delete(id) si tu implémentes delete
    }
}
