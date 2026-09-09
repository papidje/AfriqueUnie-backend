package friasoft.gn.schoolapp.dto;

import java.util.List;

public final class FamilyPreviewDtos {
    private FamilyPreviewDtos() {}

    public record SiblingStudentRow(
        Long id,
        String firstName,
        String lastName,
        String matricule,
        String className,
        String enrollmentStatus
    ) {}

    public record ParentSummary(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String email,
        String profession,
        String address,
        boolean knownInDirectory
    ) {}

    /**
     * Aperçu famille pour l’étape scolarité : jusqu’à 3 listes (commune, père seul, mère seule).
     */
    public record FamilyPreviewResponse(
        ParentSummary father,
        ParentSummary mother,
        List<SiblingStudentRow> siblingsBothParents,
        List<SiblingStudentRow> siblingsFatherOnly,
        List<SiblingStudentRow> siblingsMotherOnly
    ) {}
}
