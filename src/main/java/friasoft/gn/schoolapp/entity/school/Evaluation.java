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
@Table(name = "evaluations")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Filter(
    name = TenantHibernateFilterAspect.TENANT_FILTER_NAME,
    condition = "tenant_id = :" + TenantHibernateFilterAspect.TENANT_FILTER_PARAM
)
public class Evaluation implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "class_subject_id", nullable = false)
    private ClassSubject classSubject;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_period_id", nullable = false)
    private GradingPeriod gradingPeriod;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "eval_type", nullable = false, length = 30)
    private EvaluationType evalType;

    @Column(nullable = false)
    private Double coefficient = 1.0;

    /** Note maximale (barème), ex. 20. */
    @Column(name = "max_score", nullable = false)
    private Double maxScore = 20.0;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
