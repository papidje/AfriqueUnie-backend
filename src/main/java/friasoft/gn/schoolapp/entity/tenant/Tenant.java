package friasoft.gn.schoolapp.entity.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tenants")
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;

    private String logo;

    /** Tenant actif : faux = portail bloqué pour ses utilisateurs. Nouveaux tenants = actifs. */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /**
     * Fin d’abonnement commerciale. {@code null} = sans échéance (accès non borné dans le temps).
     */
    @Column(name = "subscription_ends_on")
    private LocalDate subscriptionEndsOn;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
