package friasoft.gn.schoolapp.entity.school;

import friasoft.gn.schoolapp.entity.auth.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fees")
public class Fee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass classRef;

    @Column(nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private FeeType name = FeeType.INSCRIPTION;

    @Column(nullable = false)
    private BigDecimal amount;

    private LocalDate dueDate;

    private String description;

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

    @OneToMany(mappedBy = "fee", cascade = CascadeType.ALL)
    private List<Payment> payments = new ArrayList<>();

    public enum FeeType {
        INSCRIPTION, SCOLARITE
    }
}

