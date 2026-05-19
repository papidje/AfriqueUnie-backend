package friasoft.gn.schoolapp.entity.auth;

import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.tenancy.TenantHibernateFilterAspect;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * Rôle et rattachement par établissement. Phase B : persisté en base ; la colonne {@code users.school_id}
 * reste la source de vérité de secours tant que la couche métier n’est pas migrée.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "user_school_affiliations",
    uniqueConstraints = @UniqueConstraint(name = "unique_user_school_role", columnNames = {"user_id", "school_id", "role"})
)
@Filter(
    name = TenantHibernateFilterAspect.TENANT_FILTER_NAME,
    condition = TenantHibernateFilterAspect.USER_SCHOOL_AFFILIATION_TENANT_FILTER_CONDITION
)
public class UserSchoolAffiliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 30, nullable = false)
    private User.UserRole role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /**
     * Si {@code false}, les autres tenants ne voient pas le nom (annuaire) tant que la personne n’a pas accepté
     * la visibilité / l’invitation pour ce rattachement.
     */
    @Column(name = "show_info_to_tenant", nullable = false)
    private boolean showInfoToTenant = false;

    /**
     * Suspension explicite par le fondateur : {@link friasoft.gn.schoolapp.service.UserService#syncUserAffiliations}
     * ne doit pas réactiver la ligne tant que ce drapeau est levé (réactivation via l’endpoint dédié ou retrait du rattachement).
     */
    @Column(name = "admin_access_suspended", nullable = false)
    private boolean adminAccessSuspended = false;
}
