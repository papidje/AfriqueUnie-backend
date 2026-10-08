package friasoft.gn.schoolapp.dto;

import java.util.List;

public final class SubjectDtos {
    private SubjectDtos() {}

    /** Création / mise à jour d’une matière du référentiel global (super-admin). */
    public record GlobalSubjectWriteRequest(
        String code,
        String name,
        /** Codes de cycles (PRE, MAT, PRI, COL, LYC) — au moins un obligatoire. */
        List<String> levelGroupCodes
    ) {}

    public record GlobalSubjectResponse(
        Long id,
        String code,
        String name,
        Long schoolId,
        List<String> levelGroupCodes
    ) {}

    public record LevelGroupOption(
        String code,
        String name
    ) {}
}
