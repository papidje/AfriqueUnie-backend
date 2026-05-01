package friasoft.gn.schoolapp.entity.school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import friasoft.gn.schoolapp.tenancy.TenantAware;
import friasoft.gn.schoolapp.tenancy.TenantHibernateFilterAspect;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "student_grading_snapshots",
    uniqueConstraints = @UniqueConstraint(columnNames = { "student_id", "grading_period_id" })
)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Filter(
    name = TenantHibernateFilterAspect.TENANT_FILTER_NAME,
    condition = "tenant_id = :" + TenantHibernateFilterAspect.TENANT_FILTER_PARAM
)
public class StudentGradingSnapshot implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_period_id", nullable = false)
    private GradingPeriod gradingPeriod;

    @Column(name = "grading_period_name", nullable = false, length = 100)
    private String gradingPeriodName;

    /** JSON : liste d’objets alignés sur {@code StudentPeriodSubjectRow}. */
    @Column(name = "subject_averages", columnDefinition = "jsonb", nullable = false)
    private String subjectAveragesJson;

    @Column(name = "period_general_average")
    private Double periodGeneralAverage;

    @Column(name = "rank_in_class")
    private Integer rankInClass;

    @Column(name = "total_evaluations", nullable = false)
    private int totalEvaluations;

    @Column(name = "composition_weight", nullable = false)
    private double compositionWeight;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;
}
