package friasoft.gn.schoolapp.dto;

import java.util.List;

public record UserResponse(
    Long id,
    String username,
    String name,
    String email,
    boolean isActive,
    List<String> roles
) {

}
