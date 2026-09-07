package friasoft.gn.schoolapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bootstrap du compte plateforme {@code SUPER_ADMIN} au démarrage.
 * Surcharge via variables d’environnement {@code APP_SUPER_ADMIN_*}.
 */
@ConfigurationProperties(prefix = "app.super-admin")
public class SuperAdminBootstrapProperties {

    /** Si false, aucun upsert au démarrage. */
    private boolean enabled = true;

    private String email = "superadmin@yopmail.com";

    private String password = "SuperAdmin123!";

    private String fullName = "Super administrateur";

    /**
     * Si true, réécrit le mot de passe à chaque démarrage avec {@link #password}.
     * Passer à false en prod une fois le mot de passe changé.
     */
    private boolean resetPasswordOnStartup = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isResetPasswordOnStartup() {
        return resetPasswordOnStartup;
    }

    public void setResetPasswordOnStartup(boolean resetPasswordOnStartup) {
        this.resetPasswordOnStartup = resetPasswordOnStartup;
    }
}
