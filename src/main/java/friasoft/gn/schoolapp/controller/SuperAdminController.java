package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.CityDtos.CityRequest;
import friasoft.gn.schoolapp.dto.CityDtos.CityResponse;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.AcceptRequest;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.AddCommentRequest;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.CommentResponse;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.RefuseRequest;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.RequestDetail;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.RequestSummary;
import friasoft.gn.schoolapp.dto.request.TenantActiveUpdateRequest;
import friasoft.gn.schoolapp.dto.response.SuperAdminGeoStatsDto;
import friasoft.gn.schoolapp.dto.response.SuperAdminSchoolDetailDto;
import friasoft.gn.schoolapp.dto.response.SuperAdminSchoolRowDto;
import friasoft.gn.schoolapp.dto.response.SuperAdminTenantDetailDto;
import friasoft.gn.schoolapp.dto.response.SuperAdminTenantRowDto;
import friasoft.gn.schoolapp.entity.school.Region;
import friasoft.gn.schoolapp.entity.school.Subject;
import friasoft.gn.schoolapp.service.CityService;
import friasoft.gn.schoolapp.service.RegionService;
import friasoft.gn.schoolapp.service.SubjectAdditionRequestService;
import friasoft.gn.schoolapp.service.SubjectService;
import friasoft.gn.schoolapp.service.SuperAdminService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("super-admin")
@AllArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final CityService cityService;
    private final RegionService regionService;
    private final SubjectService subjectService;
    private final SubjectAdditionRequestService subjectAdditionRequestService;

    @GetMapping("/tenants")
    public List<SuperAdminTenantRowDto> listTenantsWithSchools() {
        return superAdminService.listTenantsWithSchools();
    }

    @GetMapping("/tenants/{id}")
    public SuperAdminTenantDetailDto getTenantDetail(@PathVariable Long id) {
        try {
            return superAdminService.getTenantDetail(id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
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

    @GetMapping("/schools/{id}")
    public SuperAdminSchoolDetailDto getSchoolDetail(@PathVariable Long id) {
        try {
            return superAdminService.getSchoolDetail(id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
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

    @GetMapping("/class-level-groups")
    public List<friasoft.gn.schoolapp.dto.SubjectDtos.LevelGroupOption> listLevelGroupOptions() {
        return subjectService.listLevelGroupOptions();
    }

    @GetMapping("/subjects")
    public List<Subject> listGlobalSubjects() {
        return subjectService.listGlobalSubjects();
    }

    @PostMapping("/subjects")
    public Subject createGlobalSubject(@RequestBody friasoft.gn.schoolapp.dto.SubjectDtos.GlobalSubjectWriteRequest body) {
        try {
            if (body == null) {
                throw new IllegalArgumentException("Corps de requête obligatoire.");
            }
            return subjectService.createGlobal(body.code(), body.name(), body.levelGroupCodes());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PutMapping("/subjects/{id}")
    public Subject updateGlobalSubject(
        @PathVariable Long id,
        @RequestBody friasoft.gn.schoolapp.dto.SubjectDtos.GlobalSubjectWriteRequest body
    ) {
        try {
            if (body == null) {
                throw new IllegalArgumentException("Corps de requête obligatoire.");
            }
            return subjectService.updateGlobal(id, body.code(), body.name(), body.levelGroupCodes());
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

    // —— Demandes d’ajout de matière ——

    @GetMapping("/subject-addition-requests")
    public List<RequestSummary> listSubjectAdditionRequests(
        @RequestParam(required = false, defaultValue = "OPEN") String status
    ) {
        try {
            return subjectAdditionRequestService.listForSuperAdmin(status);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/subject-addition-requests/{id}")
    public RequestDetail getSubjectAdditionRequest(@PathVariable Long id) {
        return subjectAdditionRequestService.getDetail(id);
    }

    @PostMapping("/subject-addition-requests/{id}/comments")
    public CommentResponse commentSubjectAdditionRequest(
        @PathVariable Long id,
        @RequestBody AddCommentRequest body
    ) {
        try {
            return subjectAdditionRequestService.addComment(id, body);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/subject-addition-requests/{id}/accept")
    public RequestDetail acceptSubjectAdditionRequest(
        @PathVariable Long id,
        @RequestBody AcceptRequest body
    ) {
        try {
            return subjectAdditionRequestService.accept(id, body);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/subject-addition-requests/{id}/refuse")
    public RequestDetail refuseSubjectAdditionRequest(
        @PathVariable Long id,
        @RequestBody(required = false) RefuseRequest body
    ) {
        try {
            return subjectAdditionRequestService.refuse(id, body);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
