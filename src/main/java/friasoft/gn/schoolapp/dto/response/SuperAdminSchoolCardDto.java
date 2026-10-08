package friasoft.gn.schoolapp.dto.response;

/**
 * Carte école dans la fiche tenant (vue plateforme, hors données financières).
 */
public record SuperAdminSchoolCardDto(
    Long id,
    String name,
    String logo,
    boolean active,
    String cityName,
    String regionName,
    long studentCount,
    long activeYearStudentCount,
    long classCount,
    String activeYearLabel
) {}
