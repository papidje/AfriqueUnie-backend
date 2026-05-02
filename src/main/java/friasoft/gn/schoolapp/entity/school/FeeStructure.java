package friasoft.gn.schoolapp.entity.school;

import friasoft.gn.schoolapp.tenancy.TenantAware;
import friasoft.gn.schoolapp.tenancy.TenantHibernateFilterAspect;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "fee_structures",
    uniqueConstraints = @UniqueConstraint(columnNames = {"class_level_id", "school_year_id"})
)
@Filter(
    name = TenantHibernateFilterAspect.TENANT_FILTER_NAME,
    condition = "tenant_id = :" + TenantHibernateFilterAspect.TENANT_FILTER_PARAM
)
public class FeeStructure implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "class_level_id", nullable = false)
    private ClassLevel classLevel;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "school_year_id", nullable = false)
    private SchoolYear schoolYear;

    @Column(name = "registration_fee", nullable = false)
    private Double registrationFee = 0d;

    @Column(name = "re_registration_fee", nullable = false)
    private Double reRegistrationFee = 0d;

    @Column(name = "monthly_tuition_fee", nullable = false)
    private Double monthlyTuitionFee = 0d;

    @Column(name = "supplies_fee", nullable = false)
    private Double suppliesFee = 0d;

    @Column(name = "supplies_column_enabled", nullable = false)
    private Boolean suppliesColumnEnabled = false;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "GNF";

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
