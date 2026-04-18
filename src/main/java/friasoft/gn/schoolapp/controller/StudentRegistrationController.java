package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.RegistrationDTO;
import friasoft.gn.schoolapp.dto.response.StudentResponse;
import friasoft.gn.schoolapp.mapper.StudentMapper;
import friasoft.gn.schoolapp.service.StudentRegistrationService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/student-registrations")
@AllArgsConstructor
public class StudentRegistrationController {

    private final StudentRegistrationService registrationService;
    private final StudentMapper studentMapper;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF','DIRECTOR')")
    @PostMapping
    public ResponseEntity<StudentResponse> register(@RequestBody RegistrationDTO dto) {
        try {
            var saved = registrationService.registerStudent(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(studentMapper.toDto(saved));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }
}

