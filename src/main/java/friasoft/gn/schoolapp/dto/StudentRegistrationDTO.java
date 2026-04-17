package friasoft.gn.schoolapp.dto;

import java.time.LocalDate;

public record StudentRegistrationDTO(
    String civility,
    String firstName,
    String lastName,
    LocalDate birthDate,
    String emergencyContactName,
    String emergencyContactPhone
) {}

