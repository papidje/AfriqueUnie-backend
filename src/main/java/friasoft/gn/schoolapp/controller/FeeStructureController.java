package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.FeeStructureDtos.FeeStructureRequest;
import friasoft.gn.schoolapp.dto.FeeStructureDtos.FeeStructureResponse;
import friasoft.gn.schoolapp.service.FeeStructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fee-structures")
@RequiredArgsConstructor
public class FeeStructureController {

    private final FeeStructureService feeStructureService;

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE','STAFF','DIRECTOR')")
    @GetMapping
    public List<FeeStructureResponse> listBySchoolYear(@RequestParam Long schoolYearId) {
        return feeStructureService.listBySchoolYear(schoolYearId);
    }

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE','STAFF','DIRECTOR')")
    @GetMapping("/{id}")
    public FeeStructureResponse getById(@PathVariable Long id) {
        return feeStructureService.getById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE','DIRECTOR')")
    @PostMapping
    public ResponseEntity<FeeStructureResponse> create(@RequestBody FeeStructureRequest request) {
        FeeStructureResponse created = feeStructureService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE','DIRECTOR')")
    @PutMapping("/{id}")
    public FeeStructureResponse update(@PathVariable Long id, @RequestBody FeeStructureRequest request) {
        return feeStructureService.update(id, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE','DIRECTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        feeStructureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
