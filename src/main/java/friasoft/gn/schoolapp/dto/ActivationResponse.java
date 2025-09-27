package friasoft.gn.schoolapp.dto;

import java.time.Instant;

public record ActivationResponse(
    String name,
    String userName,
    String userMail,
    String userRole,
    Instant date,
    String code
) {
}