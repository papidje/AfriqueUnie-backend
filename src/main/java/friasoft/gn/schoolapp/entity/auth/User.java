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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;

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
    condition = "coalesce(tenant_id, school_id) = :" + TenantHibernateFilterAspect.TENANT_FILTER_PARAM
)
public class User implements UserDetails, TenantAware {
    /** Valeurs persistées dans {@code schools.users.role} ; JWT = {@code ROLE_} + nom d’énum. */
    public enum UserRole {
        SUPER_ADMIN,
        ADMIN_ECOLE,
        DIRECTOR,
        STAFF,
        TEACHER,
        ACCOUNTANT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullname;

    private String email;
    @Column(nullable = false)
    private String password;
    @Column(name = "tenant_id")
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 30)
    private UserRole role = UserRole.STAFF;

    private boolean isActive = false;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    /**
     * Établissement de rattachement (directeur, staff, enseignant, comptable). Absent pour le compte tenant ({@link UserRole#ADMIN_ECOLE}) et super admin.
     */
    @JoinColumn(name = "school_id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.DETACH})
    private School school;

    private Instant lastLoginAt = Instant.now();

    private String username;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        UserRole effectiveRole = this.role != null ? this.role : UserRole.STAFF;
        return java.util.List.of(new SimpleGrantedAuthority("ROLE_" + effectiveRole.name()));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.isActive;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.isActive;
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

