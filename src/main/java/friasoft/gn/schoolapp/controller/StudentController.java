package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.request.StudentPatchRequest;
import friasoft.gn.schoolapp.dto.request.StudentProfileUpdateRequest;
import friasoft.gn.schoolapp.dto.response.StudentDetailResponse;
import friasoft.gn.schoolapp.dto.response.StudentResponse;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.mapper.StudentMapper;
import friasoft.gn.schoolapp.service.IStudentService;
import friasoft.gn.schoolapp.service.document.StudentDocumentService;
import friasoft.gn.schoolapp.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final IStudentService service;
    private final StudentMapper mapper;
    private final FileStorageService fileStorageService;
    private final StudentDocumentService studentDocumentService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF','DIRECTOR','ACCOUNTANT')")
    @GetMapping
    public Page<StudentResponse> getAll(Pageable pageable) {
        return service.findAll(pageable).map(mapper::toDto);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF','DIRECTOR','ACCOUNTANT')")
    @GetMapping("/{id}")
    public ResponseEntity<StudentDetailResponse> getById(@PathVariable Long id) {
        return service.findById(id)
            .map(mapper::toDetailDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF','DIRECTOR','ACCOUNTANT')")
    @GetMapping("/by-class/{classId}")
    public List<StudentResponse> getByClass(@PathVariable Long classId) {
        try {
            return service.findByClass(classId).stream().map(mapper::toDto).toList();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE')")
    @PostMapping
    public ResponseEntity<StudentResponse> create(@RequestBody StudentResponse dto) {
        Student saved = service.save(mapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(saved));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF','DIRECTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<StudentDetailResponse> updateProfile(
        @PathVariable Long id,
        @RequestBody StudentProfileUpdateRequest request
    ) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Student updated = service.updateProfile(id, request);
            return ResponseEntity.ok(mapper.toDetailDto(updated));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF','DIRECTOR')")
    @PatchMapping("/{id}")
    public ResponseEntity<StudentDetailResponse> patchStudent(
        @PathVariable Long id,
        @RequestBody StudentPatchRequest request
    ) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Student updated = service.patchStudent(id, request);
            return ResponseEntity.ok(mapper.toDetailDto(updated));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF','DIRECTOR')")
    @PatchMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentDetailResponse> uploadPhoto(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        var existing = service.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String path = fileStorageService.storeStudentPhoto(id, file, existing.get().getPhotoPath());
        Student updated = service.updatePhotoPath(id, path);
        return ResponseEntity.ok(mapper.toDetailDto(updated));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF','DIRECTOR','ACCOUNTANT')")
    @GetMapping(value = "/{id}/documents/enrollment-certificate", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateEnrollmentCertificate(@PathVariable Long id) {
        try {
            byte[] pdf = studentDocumentService.generateEnrollmentCertificate(id);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                    .filename("attestation-inscription-eleve-" + id + ".pdf")
                    .build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF','DIRECTOR')")
    @DeleteMapping("/{id}/father")
    public ResponseEntity<Void> unlinkFather(@PathVariable Long id) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        service.unlinkFather(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF','DIRECTOR')")
    @DeleteMapping("/{id}/mother")
    public ResponseEntity<Void> unlinkMother(@PathVariable Long id) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        service.unlinkMother(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
