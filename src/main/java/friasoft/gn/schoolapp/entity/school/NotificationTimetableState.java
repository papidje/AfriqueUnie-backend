package friasoft.gn.schoolapp.entity.school;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "notification_timetable_state",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "school_class_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTimetableState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id", nullable = false)
    private SchoolClass schoolClass;

    @Column(nullable = false, length = 256)
    private String fingerprint;

    @Column(name = "change_seq", nullable = false)
    private Long changeSeq = 0L;
}
