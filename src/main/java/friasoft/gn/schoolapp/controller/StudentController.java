package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.response.StudentResponse;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.mapper.StudentMapper;
import friasoft.gn.schoolapp.service.IStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final IStudentService service;
    private final StudentMapper mapper;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF')")
    @GetMapping
    public Page<StudentResponse> getAll(Pageable pageable) {
        return service.findAll(pageable).map(mapper::toDto);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getById(@PathVariable Long id) {
        return service.findById(id)
            .map(mapper::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF')")
    @GetMapping("/by-class/{classId}")
    public List<StudentResponse> getByClass(@PathVariable Long classId) {
        try {
            return service.findByClass(classId).stream().map(mapper::toDto).toList();
        } catch (IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                e.getMessage()
            );
        }
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE')")
    @PostMapping
    public ResponseEntity<StudentResponse> create(@RequestBody StudentResponse dto) {
        Student saved = service.save(mapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(saved));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE')")
    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> update(@PathVariable Long id, @RequestBody StudentResponse dto) {
        return service.findById(id)
            .map(existing -> {
                Student entity = mapper.toEntity(dto);
                entity.setId(id);
                return ResponseEntity.ok(mapper.toDto(service.save(entity)));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

