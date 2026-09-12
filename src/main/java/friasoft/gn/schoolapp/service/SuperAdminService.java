package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.request.TenantActiveUpdateRequest;
import friasoft.gn.schoolapp.dto.response.SuperAdminGeoStatsDto;
import friasoft.gn.schoolapp.dto.response.SuperAdminGeoStatsDto.GeoCityStatsDto;
import friasoft.gn.schoolapp.dto.response.SuperAdminGeoStatsDto.GeoRegionStatsDto;
import friasoft.gn.schoolapp.dto.response.SuperAdminGeoStatsDto.GeoTotalsDto;
import friasoft.gn.schoolapp.dto.response.SuperAdminSchoolRowDto;
import friasoft.gn.schoolapp.dto.response.SuperAdminTenantRowDto;
import friasoft.gn.schoolapp.dto.response.TenantAdminSummaryDto;
import friasoft.gn.schoolapp.dto.response.TenantSchoolSummaryDto;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.City;
import friasoft.gn.schoolapp.entity.school.Region;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.tenant.Tenant;
import friasoft.gn.schoolapp.repository.ICityRepository;
import friasoft.gn.schoolapp.repository.IRegionRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import friasoft.gn.schoolapp.repository.TenantRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class SuperAdminService {

    /** Au-delà de ce seuil, l’activation exige une date de fin d’abonnement. */
    public static final long SUBSCRIPTION_REQUIRED_STUDENT_THRESHOLD = 100L;

    private final TenantRepository tenantRepository;
    private final SchoolRepository schoolRepository;
    private final IStudentRepository studentRepository;
    private final ICityRepository cityRepository;
    private final IRegionRepository regionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SuperAdminTenantRowDto> listTenantsWithSchools() {
        List<Tenant> tenants = tenantRepository.findAll();
        List<School> schools = schoolRepository.findAll();
        Map<Long, List<School>> schoolsByTenant = schools.stream()
            .filter(s -> s.getTenantId() != null)
            .collect(Collectors.groupingBy(School::getTenantId));
        Map<Long, Long> studentsByTenant = studentsByTenantId(schoolsByTenant);
        Map<Long, List<TenantAdminSummaryDto>> adminsByTenant = adminsByTenantId();

        return tenants.stream()
            .sorted(Comparator.comparing(Tenant::getId))
            .map(t -> toTenantRow(
                t,
                schoolsByTenant.getOrDefault(t.getId(), List.of()),
                studentsByTenant.getOrDefault(t.getId(), 0L),
                adminsByTenant.getOrDefault(t.getId(), List.of())
            ))
            .toList();
    }

    @Transactional
    public SuperAdminTenantRowDto setTenantActive(Long id, boolean active, TenantActiveUpdateRequest body) {
        Tenant tenant = tenantRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tenant introuvable."));

        List<School> schools = schoolRepository.findByTenantIdOrderByIdAsc(id);
        long studentCount = countStudentsForSchools(schools);

        if (active) {
            LocalDate endsOn = body != null ? body.subscriptionEndsOn() : null;
            if (studentCount > SUBSCRIPTION_REQUIRED_STUDENT_THRESHOLD) {
                if (endsOn == null) {
                    throw new IllegalStateException(
                        "Une date de fin d’abonnement est obligatoire pour activer un tenant de plus de "
                            + SUBSCRIPTION_REQUIRED_STUDENT_THRESHOLD
                            + " élèves (actuellement "
                            + studentCount
                            + ")."
                    );
                }
                if (endsOn.isBefore(LocalDate.now())) {
                    throw new IllegalStateException(
                        "La date de fin d’abonnement doit être aujourd’hui ou une date future."
                    );
                }
                tenant.setSubscriptionEndsOn(endsOn);
            } else if (endsOn != null) {
                if (endsOn.isBefore(LocalDate.now())) {
                    throw new IllegalStateException(
                        "La date de fin d’abonnement doit être aujourd’hui ou une date future."
                    );
                }
                tenant.setSubscriptionEndsOn(endsOn);
            }
        }

        tenant.setActive(active);
        tenant = tenantRepository.save(tenant);
        return toTenantRow(
            tenant,
            schools,
            studentCount,
            adminsByTenantId().getOrDefault(id, List.of())
        );
    }

    private SuperAdminTenantRowDto toTenantRow(
        Tenant t,
        List<School> schools,
        long studentCount,
        List<TenantAdminSummaryDto> admins
    ) {
        return new SuperAdminTenantRowDto(
            t.getId(),
            t.getName(),
            t.getAddress(),
            t.getLogo(),
            t.getCreatedAt(),
            t.isActive(),
            t.getSubscriptionEndsOn(),
            studentCount,
            admins,
            schools.stream()
                .sorted(Comparator.comparing(School::getId))
                .map(s -> new TenantSchoolSummaryDto(s.getId(), s.getName(), s.isActive()))
                .toList()
        );
    }

    private Map<Long, Long> studentsByTenantId(Map<Long, List<School>> schoolsByTenant) {
        Map<Long, Long> studentsBySchool = new HashMap<>();
        for (Object[] row : studentRepository.countStudentsGroupedBySchoolId()) {
            studentsBySchool.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        Map<Long, Long> byTenant = new HashMap<>();
        for (Map.Entry<Long, List<School>> e : schoolsByTenant.entrySet()) {
            long sum = 0L;
            for (School s : e.getValue()) {
                sum += studentsBySchool.getOrDefault(s.getId(), 0L);
            }
            byTenant.put(e.getKey(), sum);
        }
        return byTenant;
    }

    private long countStudentsForSchools(List<School> schools) {
        if (schools.isEmpty()) {
            return 0L;
        }
        Map<Long, Long> studentsBySchool = new HashMap<>();
        for (Object[] row : studentRepository.countStudentsGroupedBySchoolId()) {
            studentsBySchool.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        long sum = 0L;
        for (School s : schools) {
            sum += studentsBySchool.getOrDefault(s.getId(), 0L);
        }
        return sum;
    }

    private Map<Long, List<TenantAdminSummaryDto>> adminsByTenantId() {
        return userRepository.findAllOrganizationAdmins().stream()
            .filter(u -> u.getOrganizationTenantId() != null)
            .collect(Collectors.groupingBy(
                User::getOrganizationTenantId,
                Collectors.mapping(this::toAdminSummary, Collectors.toList())
            ));
    }

    private TenantAdminSummaryDto toAdminSummary(User u) {
        String fullname = u.getFullname();
        if (fullname == null || fullname.isBlank()) {
            String fn = u.getFirstName() != null ? u.getFirstName().trim() : "";
            String ln = u.getLastName() != null ? u.getLastName().trim() : "";
            fullname = (fn + " " + ln).trim();
        }
        if (fullname == null || fullname.isBlank()) {
            fullname = u.getEmail() != null ? u.getEmail() : "Administrateur";
        }
        return new TenantAdminSummaryDto(u.getId(), fullname, u.getEmail());
    }

    @Transactional(readOnly = true)
    public List<SuperAdminSchoolRowDto> listSchools() {
        Map<Long, Tenant> tenantsById = tenantRepository.findAll().stream()
            .collect(Collectors.toMap(Tenant::getId, Function.identity(), (a, b) -> a));

        Map<Long, Long> studentsBySchool = new HashMap<>();
        for (Object[] row : studentRepository.countStudentsGroupedBySchoolId()) {
            studentsBySchool.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        return schoolRepository.findAll().stream()
            .sorted(Comparator
                .comparing((School s) -> {
                    Tenant t = s.getTenantId() == null ? null : tenantsById.get(s.getTenantId());
                    return t != null ? t.getName() : "";
                }, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(School::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .map(s -> {
                Tenant tenant = s.getTenantId() == null ? null : tenantsById.get(s.getTenantId());
                City city = s.getCity();
                Region region = city != null ? city.getRegion() : null;
                return new SuperAdminSchoolRowDto(
                    s.getId(),
                    s.getName(),
                    s.getAdress(),
                    s.getContact(),
                    s.getOpenDate(),
                    s.getLogo(),
                    s.isActive(),
                    s.getCreated_at(),
                    s.getTenantId(),
                    tenant != null ? tenant.getName() : null,
                    city != null ? city.getId() : null,
                    city != null ? city.getName() : null,
                    region != null ? region.getName() : null,
                    studentsBySchool.getOrDefault(s.getId(), 0L)
                );
            })
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
