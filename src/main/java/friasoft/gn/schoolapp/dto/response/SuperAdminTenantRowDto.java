package friasoft.gn.schoolapp.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SuperAdminTenantRowDto(
    Long id,
    String name,
    String address,
    String logo,
    Instant createdAt,
    boolean active,
    LocalDate subscriptionEndsOn,
    long studentCount,
    List<TenantAdminSummaryDto> admins,
    List<TenantSchoolSummaryDto> schools
) {
}
