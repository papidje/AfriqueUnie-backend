package friasoft.gn.schoolapp.dto;

public record UserResponse(
    Short id,
    String name,
    String email,
    boolean isActive,
    String role
) {

}
