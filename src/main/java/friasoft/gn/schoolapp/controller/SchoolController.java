package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.School;
import friasoft.gn.schoolapp.service.SchoolService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("schools")
public class SchoolController {
    private final SchoolService schoolService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public void create(@RequestBody School school) {
        log.info("creation");
        this.schoolService.create(school);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping
    public List<School> getSchools() {
        return this.schoolService.getAll();
    }

    @PreAuthorize("@schoolSecurity.checkUserSchool(authentication, #schoolId)")
//    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/{schoolId}")
    public School updateSchool(@PathVariable Long schoolId, @RequestBody School dto) {
        return schoolService.update(schoolId, dto);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{schoolId}")
    public void deleteSchool(@PathVariable Long schoolId) {
        schoolService.delete(schoolId);
    }
}
