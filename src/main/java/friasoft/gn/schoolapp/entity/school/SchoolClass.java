package friasoft.gn.schoolapp.entity.school;

import friasoft.gn.schoolapp.entity.auth.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_classes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"year_id", "level_id", "name"}))
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "year_id", nullable = false)
    private SchoolYear year;

    @ManyToOne
    @JoinColumn(name = "level_id", nullable = false)
    private ClassLevel level;

    @Column(nullable = false, length = 50)
    private String name;

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

    @OneToMany(mappedBy = "classRef", cascade = CascadeType.ALL)
    private List<Enrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "classRef", cascade = CascadeType.ALL)
    private List<Fee> fees = new ArrayList<>();
}
