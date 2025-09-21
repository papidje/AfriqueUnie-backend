package friasoft.gn.schoolapp.dto;

public record UserRequest(
    String name,
    String username,
    String email,
    String password,
    short roleId,
    short schoolId
) {

}
