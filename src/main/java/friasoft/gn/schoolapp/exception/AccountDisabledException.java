package friasoft.gn.schoolapp.exception;

/**
 * Compte utilisateur désactivé ({@code users.is_active = false}) alors qu’un JWT est encore présent.
 */
public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException() {
        super("Ce compte a été désactivé. Vous ne pouvez plus utiliser SchoolApp avec cette session.");
    }
}
