package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.response.SuperAdminGeoStatsDto;
import friasoft.gn.schoolapp.dto.response.SuperAdminGeoStatsDto.GeoCityStatsDto;
import friasoft.gn.schoolapp.dto.response.SuperAdminGeoStatsDto.GeoRegionStatsDto;
import friasoft.gn.schoolapp.dto.response.SuperAdminGeoStatsDto.GeoTotalsDto;
import friasoft.gn.schoolapp.dto.response.SuperAdminTenantRowDto;
import friasoft.gn.schoolapp.dto.response.TenantSchoolSummaryDto;
import friasoft.gn.schoolapp.entity.school.City;
import friasoft.gn.schoolapp.entity.school.Region;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.tenant.Tenant;
import friasoft.gn.schoolapp.repository.ICityRepository;
import friasoft.gn.schoolapp.repository.IRegionRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import friasoft.gn.schoolapp.repository.TenantRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class SuperAdminService {

    private final TenantRepository tenantRepository;
    private final SchoolRepository schoolRepository;
    private final IStudentRepository studentRepository;
    private final ICityRepository cityRepository;
    private final IRegionRepository regionRepository;

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

    /**
     * Regroupements écoles / élèves par région et par ville (marqueurs = villes avec lat/lng).
     */
    @Transactional(readOnly = true)
    public SuperAdminGeoStatsDto geoStats() {
        Map<Long, long[]> schoolsByCity = new HashMap<>();
        for (Object[] row : schoolRepository.aggregateSchoolCountsByCity()) {
            Long cityId = ((Number) row[0]).longValue();
            long total = ((Number) row[8]).longValue();
            long active = row[9] == null ? 0L : ((Number) row[9]).longValue();
            schoolsByCity.put(cityId, new long[] { total, active });
        }

        Map<Long, Long> studentsByCity = new HashMap<>();
        for (Object[] row : studentRepository.countStudentsGroupedByCityId()) {
            Long cityId = ((Number) row[0]).longValue();
            long count = ((Number) row[1]).longValue();
            studentsByCity.put(cityId, count);
        }

        List<City> cities = cityRepository.findAllByOrderByNameAsc();
        List<GeoCityStatsDto> byCity = new ArrayList<>(cities.size());
        Map<Long, long[]> regionAgg = new HashMap<>(); // [schools, activeSchools, students]

        for (City city : cities) {
            Region region = city.getRegion();
            long[] schoolCounts = schoolsByCity.getOrDefault(city.getId(), new long[] { 0L, 0L });
            long studentCount = studentsByCity.getOrDefault(city.getId(), 0L);
            Long regionId = region != null ? region.getId() : null;
            byCity.add(new GeoCityStatsDto(
                city.getId(),
                city.getCode(),
                city.getName(),
                regionId,
                region != null ? region.getCode() : null,
                region != null ? region.getName() : null,
                city.getLatitude(),
                city.getLongitude(),
                schoolCounts[0],
                schoolCounts[1],
                studentCount
            ));
            if (regionId != null) {
                long[] agg = regionAgg.computeIfAbsent(regionId, id -> new long[] { 0L, 0L, 0L });
                agg[0] += schoolCounts[0];
                agg[1] += schoolCounts[1];
                agg[2] += studentCount;
            }
        }

        byCity.sort(Comparator
            .comparing(GeoCityStatsDto::regionName, Comparator.nullsLast(String::compareToIgnoreCase))
            .thenComparing(GeoCityStatsDto::cityName, Comparator.nullsLast(String::compareToIgnoreCase)));

        List<GeoRegionStatsDto> byRegion = regionRepository.findAllByOrderByNameAsc().stream()
            .map(r -> {
                long[] agg = regionAgg.getOrDefault(r.getId(), new long[] { 0L, 0L, 0L });
                return new GeoRegionStatsDto(
                    r.getId(),
                    r.getCode(),
                    r.getName(),
                    agg[0],
                    agg[1],
                    agg[2]
                );
            })
            .toList();

        long schoolsTotal = schoolRepository.count();
        long schoolsWithoutCity = schoolRepository.countByCityIsNull();
        long schoolsWithCity = schoolsTotal - schoolsWithoutCity;
        long studentsTotal = studentRepository.count();
        long studentsWithoutCity = studentRepository.countStudentsWithoutCity();
        long studentsWithCity = studentsTotal - studentsWithoutCity;

        return new SuperAdminGeoStatsDto(
            new GeoTotalsDto(
                schoolsTotal,
                schoolsWithCity,
                schoolsWithoutCity,
                studentsTotal,
                studentsWithCity,
                studentsWithoutCity
            ),
            byRegion,
            byCity
        );
    }
}
