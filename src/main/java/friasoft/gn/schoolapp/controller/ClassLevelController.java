package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.school.ClassLevel;
import friasoft.gn.schoolapp.service.ClassLevelService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.READ;
import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.WRITE;

@RestController
@RequestMapping("/api/class-levels")
@AllArgsConstructor
public class ClassLevelController {

    private final ClassLevelService service;

    @PreAuthorize(READ)
    @GetMapping
    public List<ClassLevel> getAll() {
        return service.findAll();
    }

    @PreAuthorize(READ)
    @GetMapping("/group/{groupCode}")
    public List<ClassLevel> getByGroup(@PathVariable String groupCode) {
        return service.findByGroup(groupCode);
    }

    @PreAuthorize(WRITE)
    @PostMapping
    public ClassLevel create(@RequestBody ClassLevel level) {
        return service.save(level);
    }
}
