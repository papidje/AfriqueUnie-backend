package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.school.City;
import friasoft.gn.schoolapp.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.READ;

/**
 * Liste des villes actives pour les formulaires établissement (référentiel global).
 */
@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @PreAuthorize(READ)
    @GetMapping
    public List<City> listActive() {
        return cityService.listActive();
    }
}
