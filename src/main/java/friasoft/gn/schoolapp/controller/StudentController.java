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

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final IStudentService service;
    private final StudentMapper mapper;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','TEACHER')")
    @GetMapping
    public Page<StudentResponse> getAll(Pageable pageable) {
        return service.findAll(pageable).map(mapper::toDto);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','TEACHER')")
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getById(@PathVariable Long id) {
        return service.findById(id)
            .map(mapper::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
    @PostMapping
    public ResponseEntity<StudentResponse> create(@RequestBody StudentResponse dto) {
        Student saved = service.save(mapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(saved));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
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

