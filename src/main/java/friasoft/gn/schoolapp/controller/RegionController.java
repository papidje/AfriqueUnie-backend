package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.school.Region;
import friasoft.gn.schoolapp.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.READ;

/**
 * Liste des régions actives (référentiel géographique).
 */
@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @PreAuthorize(READ)
    @GetMapping
    public List<Region> listActive() {
        return regionService.listActive();
    }
}
