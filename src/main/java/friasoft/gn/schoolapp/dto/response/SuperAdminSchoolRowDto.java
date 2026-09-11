package friasoft.gn.schoolapp.dto.response;

import java.sql.Date;
import java.time.Instant;

/**
 * Ligne enrichie pour la liste plateforme des écoles (SuperAdmin).
 */
public record SuperAdminSchoolRowDto(
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
    Long cityId,
    String cityName,
    String regionName,
    long studentCount
) {}
