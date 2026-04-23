package friasoft.gn.schoolapp.dto.request;

import java.time.LocalDate;

public record StudentPatchRequest(
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
    String allergies,
    String tutorName,
    String tutorProfession,
    String tutorPhone,
    String tutorEmail,
    String enrollmentStatus,
    String classHistory
) {}
