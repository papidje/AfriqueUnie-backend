package friasoft.gn.schoolapp.security;

/**
 * Expressions {@code @PreAuthorize} pour l’UI scolaire (classes, matières, années, etc.).
 * <ul>
 *   <li><strong>READ</strong> : accès en lecture pour enseignants et comptable en plus du personnel.</li>
 *   <li><strong>WRITE</strong> : modification réservée à l’administration (pas d’enseignant).</li>
 * </ul>
 */
public final class SchoolUiSecurityExpressions {

    private SchoolUiSecurityExpressions() {
    }

    public static final String READ =
        "hasAnyRole('ADMIN_ECOLE','DIRECTOR','STAFF','TEACHER','ACCOUNTANT')";

    public static final String WRITE =
        "hasAnyRole('ADMIN_ECOLE','DIRECTOR','STAFF')";

    /** Création / saisie notes : enseignants inclus. */
    public static final String WRITE_OR_TEACHER =
        "hasAnyRole('ADMIN_ECOLE','DIRECTOR','STAFF','TEACHER')";
}

