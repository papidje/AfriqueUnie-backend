package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.school.ClassLevel;
import friasoft.gn.schoolapp.service.ClassLevelService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/class-levels")
@AllArgsConstructor
public class ClassLevelController {

    private final ClassLevelService service;

    @GetMapping
    public List<ClassLevel> getAll() {
        return service.findAll();
    }

    @GetMapping("/group/{groupCode}")
    public List<ClassLevel> getByGroup(@PathVariable String groupCode) {
        return service.findByGroup(groupCode);
    }

    @PostMapping
    public ClassLevel create(@RequestBody ClassLevel level) {
        return service.save(level);
    }
}
