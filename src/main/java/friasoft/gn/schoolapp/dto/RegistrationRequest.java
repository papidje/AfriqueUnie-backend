package friasoft.gn.schoolapp.dto;

public record RegistrationRequest(
    String username,
    String fullname,
    String email,
    String tenantName,
    String schoolName,
    String tenantAddress,
    String tenantLogo
) {
}
