package friasoft.gn.schoolapp.dto;

import java.util.List;

public record UserRequest(
    String username,
    String fullname,
    String email,
    String password,
    List<Long> rolesId,
    Long schoolId
) {

}
