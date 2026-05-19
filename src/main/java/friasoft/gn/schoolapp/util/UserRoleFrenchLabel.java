package friasoft.gn.schoolapp.util;

import friasoft.gn.schoolapp.entity.auth.User;

/** Libellés français des rôles — à garder alignés avec {@code AfriqueUnieApp/src/app/core/role-labels.ts}. */
public final class UserRoleFrenchLabel {

    private UserRoleFrenchLabel() {}

    public static String format(User.UserRole role) {
        if (role == null) {
            return "Utilisateur";
        }
        return switch (role) {
            case SUPER_ADMIN -> "Super administrateur";
            case ADMIN_ECOLE -> "Administrateur d’école";
            case DIRECTOR -> "Directeur·rice";
            case STAFF -> "Personnel administratif";
            case TEACHER -> "Enseignant(e)";
        };
    }
}
