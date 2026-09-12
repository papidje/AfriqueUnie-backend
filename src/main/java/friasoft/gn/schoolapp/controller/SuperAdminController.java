package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.CityDtos.CityRequest;
import friasoft.gn.schoolapp.dto.CityDtos.CityResponse;
import friasoft.gn.schoolapp.dto.request.TenantActiveUpdateRequest;
import friasoft.gn.schoolapp.dto.response.SuperAdminGeoStatsDto;
import friasoft.gn.schoolapp.dto.response.SuperAdminSchoolRowDto;
import friasoft.gn.schoolapp.dto.response.SuperAdminTenantRowDto;
import friasoft.gn.schoolapp.entity.school.Region;
import friasoft.gn.schoolapp.entity.school.Subject;
import friasoft.gn.schoolapp.service.CityService;
import friasoft.gn.schoolapp.service.RegionService;
import friasoft.gn.schoolapp.service.SubjectService;
import friasoft.gn.schoolapp.service.SuperAdminService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("super-admin")
@AllArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final CityService cityService;
    private final RegionService regionService;
    private final SubjectService subjectService;

    @GetMapping("/tenants")
    public List<SuperAdminTenantRowDto> listTenantsWithSchools() {
        return superAdminService.listTenantsWithSchools();
    }

    @PatchMapping("/tenants/{id}/active/{active}")
    public SuperAdminTenantRowDto setTenantActive(
        @PathVariable Long id,
        @PathVariable boolean active,
        @RequestBody(required = false) TenantActiveUpdateRequest body
    ) {
        try {
            return superAdminService.setTenantActive(id, active, body);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/schools")
    public List<SuperAdminSchoolRowDto> listSchools() {
        return superAdminService.listSchools();
    }

    /** P2/P3 — agrégats écoles/élèves par région et par ville (+ coords pour carte). */
    @GetMapping("/geo/stats")
    public SuperAdminGeoStatsDto geoStats() {
        return superAdminService.geoStats();
    }

    // —— Régions ——

    @GetMapping("/regions")
    public List<Region> listRegions() {
        return regionService.listAllForAdmin();
    }

    // —— Villes ——

    @GetMapping("/cities")
    public List<CityResponse> listCities() {
        return cityService.listAllForAdmin();
    }

    @PostMapping("/cities")
    public CityResponse createCity(@RequestBody CityRequest body) {
        try {
            return cityService.create(body);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PutMapping("/cities/{id}")
    public CityResponse updateCity(@PathVariable Long id, @RequestBody CityRequest body) {
        try {
            return cityService.update(id, body);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                e.getMessage() != null && e.getMessage().contains("introuvable")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST,
                e.getMessage()
            );
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PatchMapping("/cities/{id}/active/{active}")
    public CityResponse setCityActive(@PathVariable Long id, @PathVariable boolean active) {
        try {
            return cityService.setActive(id, active);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/cities/{id}")
    public void deleteCity(@PathVariable Long id) {
        try {
            cityService.delete(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    // —— Matières (référentiel global) ——

    @GetMapping("/subjects")
    public List<Subject> listGlobalSubjects() {
        return subjectService.listGlobalSubjects();
    }

    @PostMapping("/subjects")
    public Subject createGlobalSubject(@RequestBody Map<String, String> body) {
        try {
            return subjectService.createGlobal(body.get("code"), body.get("name"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PutMapping("/subjects/{id}")
    public Subject updateGlobalSubject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            return subjectService.updateGlobal(id, body.get("code"), body.get("name"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                e.getMessage() != null && e.getMessage().contains("introuvable")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST,
                e.getMessage()
            );
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @DeleteMapping("/subjects/{id}")
    public void deleteGlobalSubject(@PathVariable Long id) {
        try {
            subjectService.deleteGlobal(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }
}
