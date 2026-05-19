package friasoft.gn.schoolapp.entity.auth;

import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.tenancy.TenantAware;
import friasoft.gn.schoolapp.tenancy.TenantHibernateFilterAspect;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
@FilterDef(
    name = TenantHibernateFilterAspect.TENANT_FILTER_NAME,
    parameters = @ParamDef(name = TenantHibernateFilterAspect.TENANT_FILTER_PARAM, type = Long.class)
)
@Filter(
    name = TenantHibernateFilterAspect.TENANT_FILTER_NAME,
    condition = TenantHibernateFilterAspect.USER_TABLE_TENANT_FILTER_CONDITION
)
public class User implements UserDetails, TenantAware {
    /** Rôles métier par école {@link UserSchoolAffiliation} ; rôles plateforme {@link UserPlatformRole}. JWT = {@code ROLE_} + nom. */
    public enum UserRole {
        SUPER_ADMIN,
        ADMIN_ECOLE,
        DIRECTOR,
        STAFF,
        TEACHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullname;

    /**
     * Unicité garantie en base par l’index {@code uk_users_email_lower} sur {@code lower(trim(email))}
     * (voir Liquibase {@code v0_038.sql}) — pas de doublon pour un même e-mail, une fois normalisé en casse / espaces.
     */
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(name = "tenant_id")
    private Long tenantId;

    private boolean isActive = false;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    /**
     * Établissement de rattachement principal (navigation, directeurs, multi-affiliations).
     */
    @JoinColumn(name = "school_id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.DETACH})
    private School school;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<UserSchoolAffiliation> affiliations = new ArrayList<>();

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private UserPlatformRole platformRole;

    private Instant lastLoginAt = Instant.now();

    private String username;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        /* Les autorités effectives viennent du JWT ({@link friasoft.gn.schoolapp.security.JwtService}). */
        return List.of();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    public String getUsername() {
        return this.email;
    }

    /**
     * Seul {@link #isEnabled()} reflète {@link #isActive} (compte activé par mail).
     * Les autres indicateurs {@link UserDetails} sont à {@code true} tant qu’on n’a pas de verrouillage
     * / expiration métier dédiés ; sinon un compte inactif déclenche {@link org.springframework.security.authentication.LockedException}
     * avant {@link org.springframework.security.authentication.DisabledException} (ordre Spring Security).
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.isActive;
    }

    /** Valeur brute {@code tenant_id} en base (organisation), sans logique de repli. */
    public Long getOrganizationTenantId() {
        return this.tenantId;
    }

    @Override
    @Transient
    public Long getTenantId() {
        if (this.tenantId != null) {
            return this.tenantId;
        }
        if (this.school != null && this.school.getTenantId() != null) {
            return this.school.getTenantId();
        }
        return null;
    }

    @Override
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
        if (tenantId == null) {
            this.school = null;
        }
    }
}

