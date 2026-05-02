package friasoft.gn.schoolapp.entity.school;

import friasoft.gn.schoolapp.tenancy.TenantAware;
import friasoft.gn.schoolapp.tenancy.TenantHibernateFilterAspect;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "parents")
@Filter(
    name = TenantHibernateFilterAspect.TENANT_FILTER_NAME,
    condition = "tenant_id = :" + TenantHibernateFilterAspect.TENANT_FILTER_PARAM
)
public class Parent implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 40)
    private String phone;

    @Column(length = 180)
    private String email;

    @Column(length = 150)
    private String profession;

    @Column(length = 255)
    private String address;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
