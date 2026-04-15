package friasoft.gn.schoolapp.dto.response;

import java.time.Instant;
import java.util.List;

public record SuperAdminTenantRowDto(
    Long id,
    String name,
    String address,
    String logo,
    Instant createdAt,
    List<TenantSchoolSummaryDto> schools
) {
}
