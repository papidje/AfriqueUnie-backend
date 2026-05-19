package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.ClassSubjectDtos.TeacherSummaryResponse;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.service.SchoolService;
import friasoft.gn.schoolapp.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("schools")
public class SchoolController {
    private final SchoolService schoolService;
    private final FileStorageService fileStorageService;

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE', 'DIRECTOR')")
    @PostMapping
    public ResponseEntity<School> create(@RequestBody School school) {
        log.info("creation école");
        School saved = this.schoolService.create(school);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE', 'DIRECTOR', 'STAFF', 'TEACHER')")
    @GetMapping
    public List<School> getSchools() {
        return this.schoolService.listForAuthenticatedUser();
    }

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE', 'DIRECTOR') and @schoolSecurity.checkUserSchool(authentication, #schoolId)")
    @PutMapping("/{schoolId}")
    public School updateSchool(@PathVariable Long schoolId, @RequestBody School dto) {
        return schoolService.update(schoolId, dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE', 'DIRECTOR') and @schoolSecurity.checkUserSchool(authentication, #schoolId)")
    @PatchMapping(value = "/{schoolId}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public School uploadLogo(@PathVariable Long schoolId, @RequestPart("file") MultipartFile file) {
        School existing = schoolService.getSchool(schoolId);
        String path = fileStorageService.storeSchoolLogo(schoolId, file, existing.getLogo());
        return schoolService.updateLogoPath(schoolId, path);
    }

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE', 'DIRECTOR')")
    @DeleteMapping("/{schoolId}")
    public void deleteSchool(@PathVariable Long schoolId) {
        schoolService.delete(schoolId);
    }

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE', 'DIRECTOR')")
    @PatchMapping("/{schoolId}/active/{active}")
    public void activateSchool(@PathVariable Long schoolId, @PathVariable boolean active) {
        schoolService.activate(schoolId, active);
    }

    @PreAuthorize("@schoolSecurity.checkUserSchool(authentication, #schoolId)")
    @GetMapping("/{schoolId}")
    public School getSchool(@PathVariable Long schoolId) {
        return schoolService.getSchool(schoolId);
    }

    @PreAuthorize("@schoolSecurity.checkUserSchool(authentication, #schoolId)")
    @GetMapping("/{schoolId}/teachers")
    public List<TeacherSummaryResponse> listTeachers(@PathVariable Long schoolId) {
        return schoolService.listTeachersForSchool(schoolId);
    }

}
