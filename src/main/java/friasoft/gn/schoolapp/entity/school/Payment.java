package friasoft.gn.schoolapp.entity.school;

import friasoft.gn.schoolapp.entity.auth.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"enrollment_id", "fee_id"}))
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @ManyToOne
    @JoinColumn(name = "fee_id", nullable = false)
    private Fee fee;

    @Column(nullable = false)
    private BigDecimal paidAmount;

    @CreationTimestamp
    private LocalDateTime paymentDate;

    @Column(length = 50)
    private String paymentMethod;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.PAID;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    public enum Status {
        PAID, PARTIAL, PENDING
    }
}

