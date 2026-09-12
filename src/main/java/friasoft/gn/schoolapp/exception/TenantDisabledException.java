package friasoft.gn.schoolapp.exception;

/**
 * Organisation (tenant) désactivée — session ou connexion refusées pour les utilisateurs rattachés.
 */
public class TenantDisabledException extends RuntimeException {

    public TenantDisabledException() {
        super(
            "Cette organisation a été désactivée. Vous ne pouvez plus utiliser SchoolApp pour ses établissements."
        );
    }
}
