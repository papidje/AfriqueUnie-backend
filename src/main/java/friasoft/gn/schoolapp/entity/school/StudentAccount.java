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
    name = "student_accounts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "school_year_id"})
)
@Filter(
    name = TenantHibernateFilterAspect.TENANT_FILTER_NAME,
    condition = "tenant_id = :" + TenantHibernateFilterAspect.TENANT_FILTER_PARAM
)
public class StudentAccount implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "school_year_id", nullable = false)
    private SchoolYear schoolYear;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "GNF";

    @Column(name = "supplies_paid", nullable = false)
    private Boolean suppliesPaid = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

