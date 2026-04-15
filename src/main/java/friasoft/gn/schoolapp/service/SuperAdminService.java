package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.response.SuperAdminTenantRowDto;
import friasoft.gn.schoolapp.dto.response.TenantSchoolSummaryDto;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.tenant.Tenant;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import friasoft.gn.schoolapp.repository.TenantRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class SuperAdminService {

    private final TenantRepository tenantRepository;
    private final SchoolRepository schoolRepository;

    @Transactional(readOnly = true)
    public List<SuperAdminTenantRowDto> listTenantsWithSchools() {
        List<Tenant> tenants = tenantRepository.findAll();
        List<School> schools = schoolRepository.findAll();
        Map<Long, List<School>> schoolsByTenant = schools.stream()
            .filter(s -> s.getTenantId() != null)
            .collect(Collectors.groupingBy(School::getTenantId));

        return tenants.stream()
            .sorted(Comparator.comparing(Tenant::getId))
            .map(t -> new SuperAdminTenantRowDto(
                t.getId(),
                t.getName(),
                t.getAddress(),
                t.getLogo(),
                t.getCreatedAt(),
                schoolsByTenant.getOrDefault(t.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(School::getId))
                    .map(s -> new TenantSchoolSummaryDto(s.getId(), s.getName(), s.isActive()))
                    .toList()
            ))
            .toList();
    }
}
