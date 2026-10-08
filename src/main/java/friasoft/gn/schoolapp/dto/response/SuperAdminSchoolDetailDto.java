package friasoft.gn.schoolapp.dto.response;

import java.sql.Date;
import java.time.Instant;
import java.util.List;

/**
 * Fiche établissement plateforme — identité et volumes opérationnels (pas de finance).
 */
public record SuperAdminSchoolDetailDto(
    Long id,
    String name,
    String adress,
    String contact,
    Date openDate,
    String logo,
    boolean active,
    Instant createdAt,
    Long tenantId,
    String tenantName,
    boolean tenantActive,
    Long cityId,
    String cityName,
    String regionName,
    /** Effectif total (toutes années / fiches). */
    long studentCount,
    /** Élèves rattachés à une classe de l’année active. */
    long activeYearStudentCount,
    long classCount,
    long capacity,
    long staffCount,
    long teacherCount,
    String activeYearLabel,
    List<SchoolYearSummaryDto> schoolYears
) {
    public record SchoolYearSummaryDto(
        Long id,
        String label,
        boolean active
    ) {}
}
