package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.service.SchoolClassService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public SchoolClass create(@RequestBody SchoolClass schoolClass) {
        return service.save(schoolClass);
    }
}

