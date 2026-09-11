package friasoft.gn.schoolapp.dto;

public final class SchoolClassDtos {

    private SchoolClassDtos() {}

    /**
     * Mise à jour des métadonnées d’une classe (année et type de période inchangés).
     */
    public record UpdateSchoolClassRequest(
        String name,
        Long levelId,
        Integer capacity
    ) {}
}
