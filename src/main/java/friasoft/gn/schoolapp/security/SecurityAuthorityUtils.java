package friasoft.gn.schoolapp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Autorités JWT ({@code ROLE_*}) présentes dans le {@link SecurityContextHolder}
 * après {@link JwtFilter} pour l’établissement actif.
 */
public final class SecurityAuthorityUtils {

    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String ROLE_ADMIN_ECOLE = "ROLE_ADMIN_ECOLE";
    public static final String ROLE_DIRECTOR = "ROLE_DIRECTOR";
    public static final String ROLE_TEACHER = "ROLE_TEACHER";
    public static final String ROLE_STAFF = "ROLE_STAFF";

    private SecurityAuthorityUtils() {
    }

    public static Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static boolean hasAuthority(String authority) {
        Authentication auth = authentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    public static boolean hasAnyAuthority(String... authorities) {
        Authentication auth = authentication();
        if (auth == null || authorities.length == 0) {
            return false;
        }
        for (String wanted : authorities) {
            if (auth.getAuthorities().stream().anyMatch(a -> wanted.equals(a.getAuthority()))) {
                return true;
            }
        }
        return false;
    }
}
