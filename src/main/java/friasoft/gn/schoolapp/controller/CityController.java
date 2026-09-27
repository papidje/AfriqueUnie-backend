package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.school.City;
import friasoft.gn.schoolapp.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Liste des villes actives pour les formulaires établissement (référentiel global).
 * Public : utilisée aussi à l’inscription d’une école (sans JWT).
 */
@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @GetMapping
    public List<City> listActive() {
        return cityService.listActive();
    }
}
