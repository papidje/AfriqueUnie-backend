package friasoft.gn.schoolapp.dto.response;

import java.util.List;

/**
 * Agrégats géographiques plateforme (SuperAdmin) — écoles / élèves par région et par ville.
 */
public record SuperAdminGeoStatsDto(
    GeoTotalsDto totals,
    List<GeoRegionStatsDto> byRegion,
    List<GeoCityStatsDto> byCity
) {
    public record GeoTotalsDto(
        long schools,
        long schoolsWithCity,
        long schoolsWithoutCity,
        long students,
        long studentsWithCity,
        long studentsWithoutCity
    ) {}

    public record GeoRegionStatsDto(
        Long regionId,
        String regionCode,
        String regionName,
        long schoolCount,
        long activeSchoolCount,
        long studentCount
    ) {}

    /** Une ligne = une ville (coords pour marqueurs carte). */
    public record GeoCityStatsDto(
        Long cityId,
        String cityCode,
        String cityName,
        Long regionId,
        String regionCode,
        String regionName,
        Double latitude,
        Double longitude,
        long schoolCount,
        long activeSchoolCount,
        long studentCount
    ) {}
}
