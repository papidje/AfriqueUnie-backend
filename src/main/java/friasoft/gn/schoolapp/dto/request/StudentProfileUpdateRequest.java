package friasoft.gn.schoolapp.dto.request;

import java.time.LocalDate;

/**
 * Mise à jour des informations élève (hors parents et matricule).
 */
public record StudentProfileUpdateRequest(
    String civility,
    String firstName,
    String lastName,
    LocalDate birthDate,
    String emergencyContactName,
    String emergencyContactPhone
) {}
