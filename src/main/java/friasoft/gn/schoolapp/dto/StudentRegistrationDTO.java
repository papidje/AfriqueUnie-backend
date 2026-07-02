package friasoft.gn.schoolapp.dto;

import java.time.LocalDate;

public record StudentRegistrationDTO(
    String civility,
    String firstName,
    String lastName,
    LocalDate birthDate,
    String birthPlace,
    String nationality,
    String address,
    String communicationPhone,
    String communicationEmail,
    String emergencyContactName,
    String emergencyContactPhone,
    String bloodGroup,
    String allergies
) {}

