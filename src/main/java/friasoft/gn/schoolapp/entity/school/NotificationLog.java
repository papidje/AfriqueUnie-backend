package friasoft.gn.schoolapp.entity.school;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
}
