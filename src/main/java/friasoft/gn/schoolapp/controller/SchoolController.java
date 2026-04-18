package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.ClassSubjectDtos.TeacherSummaryResponse;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.service.SchoolService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("schools")
public class SchoolController {
    private final SchoolService schoolService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_ECOLE')")
    @PostMapping
    public ResponseEntity<School> create(@RequestBody School school) {
        log.info("creation école");
        School saved = this.schoolService.create(school);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_ECOLE', 'DIRECTOR', 'STAFF', 'TEACHER', 'ACCOUNTANT')")
    @GetMapping
    public List<School> getSchools() {
        return this.schoolService.listForAuthenticatedUser();
    }

    @PreAuthorize("@schoolSecurity.checkUserSchool(authentication, #schoolId)")
    @PutMapping("/{schoolId}")
    public School updateSchool(@PathVariable Long schoolId, @RequestBody School dto) {
        return schoolService.update(schoolId, dto);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_ECOLE')")
    @DeleteMapping("/{schoolId}")
    public void deleteSchool(@PathVariable Long schoolId) {
        schoolService.delete(schoolId);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_ECOLE')")
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
