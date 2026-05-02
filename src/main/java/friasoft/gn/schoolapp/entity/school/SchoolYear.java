package friasoft.gn.schoolapp.entity.school;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.tenancy.TenantAware;
import friasoft.gn.schoolapp.tenancy.TenantHibernateFilterAspect;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_years",
    uniqueConstraints = @UniqueConstraint(columnNames = {"school_id", "label"}))
@Filter(
    name = TenantHibernateFilterAspect.TENANT_FILTER_NAME,
    condition = "tenant_id = :" + TenantHibernateFilterAspect.TENANT_FILTER_PARAM
)
public class SchoolYear implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tenant organisation (même sémantique que {@link School#getTenantId()} et le JWT {@code tenant_id}). */
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private School school;

    @Column(nullable = false, length = 20)
    private String label;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Non exposé en JSON : références Hibernate {@code getReferenceById} → proxies non sérialisables. */
    @ManyToOne
    @JoinColumn(name = "created_by")
    @JsonIgnore
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "updated_by")
    @JsonIgnore
    private User updatedBy;

    @OneToMany(mappedBy = "year", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<SchoolClass> classes = new ArrayList<>();
}
