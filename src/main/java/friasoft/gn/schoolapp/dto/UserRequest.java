package friasoft.gn.schoolapp.dto;

import friasoft.gn.schoolapp.entity.auth.User;

public record UserRequest(
    String username,
    String fullname,
    String email,
    String password,
    User.UserRole role,
    Long schoolId
) {

}
