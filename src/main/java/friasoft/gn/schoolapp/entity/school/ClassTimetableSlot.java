package friasoft.gn.schoolapp.entity.school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    name = "class_timetable_slots",
    uniqueConstraints = @UniqueConstraint(columnNames = {"class_id", "day_of_week", "slot_index"})
)
@Filter(
    name = TenantHibernateFilterAspect.TENANT_FILTER_NAME,
    condition = "tenant_id = :" + TenantHibernateFilterAspect.TENANT_FILTER_PARAM
)
public class ClassTimetableSlot implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    @JsonIgnoreProperties({"enrollments", "fees", "year", "level", "createdBy", "updatedBy"})
    private SchoolClass schoolClass;

    /** 1 = lundi … 5 = vendredi */
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    /** 0 = 08:00–09:00 … 7 = 15:00–16:00 */
    @Column(name = "slot_index", nullable = false)
    private Integer slotIndex;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "class_subject_id", nullable = false)
    @JsonIgnoreProperties({"schoolClass", "subject", "teacher", "hibernateLazyInitializer", "handler"})
    private ClassSubject classSubject;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
