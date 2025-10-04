package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.school.Enrollment;
import friasoft.gn.schoolapp.service.EnrollmentService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@AllArgsConstructor
public class EnrollmentController {

    private final EnrollmentService service;

    @GetMapping("/{id}")
    public ResponseEntity<Enrollment> getById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/student/{studentId}")
    public List<Enrollment> getByStudent(@PathVariable Long studentId) {
        return service.findByStudent(studentId);
    }

    @GetMapping("/class/{classId}")
    public List<Enrollment> getByClass(@PathVariable Long classId) {
        return service.findByClass(classId);
    }

    @PostMapping
    public Enrollment create(@RequestBody Enrollment enrollment) {
        return service.save(enrollment);
    }
}

