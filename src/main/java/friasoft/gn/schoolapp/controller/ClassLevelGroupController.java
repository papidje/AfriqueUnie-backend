package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.school.ClassLevelGroup;
import friasoft.gn.schoolapp.service.ClassLevelGroupService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.READ;
import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.WRITE;

@RestController
@RequestMapping("/api/class-level-groups")
@AllArgsConstructor
public class ClassLevelGroupController {

    private ClassLevelGroupService service;

    @PreAuthorize(READ)
    @GetMapping
    public List<ClassLevelGroup> getAll() {
        return service.findAll();
    }

    @PreAuthorize(WRITE)
    @PostMapping
    public ClassLevelGroup create(@RequestBody ClassLevelGroup group) {
        return service.save(group);
    }
}

