package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.response.SuperAdminTenantRowDto;
import friasoft.gn.schoolapp.service.SuperAdminService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("super-admin")
@AllArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @GetMapping("/tenants")
    public List<SuperAdminTenantRowDto> listTenantsWithSchools() {
        return superAdminService.listTenantsWithSchools();
    }
}
