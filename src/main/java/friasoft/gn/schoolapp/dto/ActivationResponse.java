package friasoft.gn.schoolapp.dto;

public record ActivationResponse(
    String userName,
    String userMail,
    String userRole,
    java.sql.Date date,
    String code
) {
}