package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.ParentDtos.ParentResponse;
import friasoft.gn.schoolapp.dto.ParentSchoolListRow;
import friasoft.gn.schoolapp.dto.ParentDtos.ParentWriteRequest;
import friasoft.gn.schoolapp.entity.school.Parent;
import friasoft.gn.schoolapp.service.ParentService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/parents")
@AllArgsConstructor
public class ParentController {

    private final ParentService parentService;

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE','STAFF','DIRECTOR','ACCOUNTANT')")
    @GetMapping("/by-school/{schoolId}/active-year-enrolled")
    public List<ParentSchoolListRow> listForSchoolActiveYear(@PathVariable Long schoolId) {
        try {
            return parentService.listForSchoolActiveYear(schoolId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE','STAFF','DIRECTOR')")
    @GetMapping("/by-phone")
    public ResponseEntity<ParentResponse> findByPhone(@RequestParam String phone) {
        try {
            return parentService.findByPhone(phone)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE','STAFF','DIRECTOR')")
    @GetMapping("/{id}")
    public ResponseEntity<ParentResponse> getById(@PathVariable Long id) {
        return parentService.findById(id)
            .map(this::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE','STAFF','DIRECTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<ParentResponse> update(@PathVariable Long id, @RequestBody ParentWriteRequest body) {
        try {
            Parent saved = parentService.update(id, body);
            return ResponseEntity.ok(toResponse(saved));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE','STAFF','DIRECTOR')")
    @PostMapping
    public ResponseEntity<ParentResponse> create(@RequestBody ParentWriteRequest body) {
        try {
            Parent parent = new Parent();
            parent.setLastName(requireNonBlank(body.lastName(), "Nom obligatoire."));
            parent.setFirstName(requireNonBlank(body.firstName(), "Prénom obligatoire."));
            parent.setPhone(body.phone());
            parent.setEmail(trimToNull(body.email()));
            parent.setProfession(trimToNull(body.profession()));
            parent.setAddress(trimToNull(body.address()));
            Parent saved = parentService.save(parent);
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    private ParentResponse toResponse(Parent p) {
        return new ParentResponse(
            p.getId(),
            p.getTenantId(),
            p.getLastName(),
            p.getFirstName(),
            p.getPhone(),
            p.getEmail(),
            p.getProfession(),
            p.getAddress()
        );
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String out = value.trim();
        return out.isEmpty() ? null : out;
    }
}
