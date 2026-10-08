package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.school.ClassLevelGroup;
import friasoft.gn.schoolapp.service.ClassLevelGroupService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.READ;

/**
 * Lecture du référentiel des cycles pour l’UI école.
 * Écriture réservée au super-admin ({@code /super-admin/class-level-groups}).
 */
@RestController
@RequestMapping("/api/class-level-groups")
@AllArgsConstructor
public class ClassLevelGroupController {

    private final ClassLevelGroupService service;

    @PreAuthorize(READ)
    @GetMapping
    public List<ClassLevelGroup> getAll() {
        return service.findAll();
    }
}
