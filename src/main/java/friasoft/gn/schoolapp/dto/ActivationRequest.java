package friasoft.gn.schoolapp.dto;

public record ActivationRequest(
    String email,
    String activationCode,
    String newPassword
) {

}
