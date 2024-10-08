package friasoft.gn.schoolapp.dto;

public record ActivationRequest(
    String userMail,
    String code
) {

}
