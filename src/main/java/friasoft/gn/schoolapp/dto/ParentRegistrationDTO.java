package friasoft.gn.schoolapp.dto;

public record ParentRegistrationDTO(
    String lastName,
    String firstName,
    String phone,
    String email,
    String profession,
    String address
) {}

