package friasoft.gn.schoolapp.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Fiche tenant plateforme — volumes et structure, sans montants financiers.
 */
public record SuperAdminTenantDetailDto(
    Long id,
    String name,
    String address,
    String logo,
    Instant createdAt,
    boolean active,
    LocalDate subscriptionEndsOn,
    long studentCount,
    long schoolCount,
    long activeSchoolCount,
    List<TenantAdminSummaryDto> admins,
    List<SuperAdminSchoolCardDto> schools
) {}
