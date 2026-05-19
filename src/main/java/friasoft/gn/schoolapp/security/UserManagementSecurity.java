package friasoft.gn.schoolapp.security;

import friasoft.gn.schoolapp.entity.auth.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Droits « gestion du personnel » : alignés sur les autorités JWT (rôles effectifs pour l’établissement / organisation active).
 */
@Component("userMgmtSecurity")
public class UserManagementSecurity {

    public boolean canManageDirectory(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            return false;
        }
        return hasAuthority(auth, "ROLE_ADMIN_ECOLE") || hasAuthority(auth, "ROLE_DIRECTOR");
    }

    /** Édition des affiliations multi-écoles : réservée au fondateur. */
    public boolean canManageAffiliationsAsFounder(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            return false;
        }
        return hasAuthority(auth, "ROLE_ADMIN_ECOLE");
    }

    private static boolean hasAuthority(Authentication auth, String authority) {
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (authority.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
