package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.school.ClassLevelGroup;
import friasoft.gn.schoolapp.service.ClassLevelGroupService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/class-level-groups")
@AllArgsConstructor
public class ClassLevelGroupController {

    private ClassLevelGroupService service;

    @GetMapping
    public List<ClassLevelGroup> getAll() {
        return service.findAll();
    }

    @PostMapping
    public ClassLevelGroup create(@RequestBody ClassLevelGroup group) {
        return service.save(group);
    }
}

