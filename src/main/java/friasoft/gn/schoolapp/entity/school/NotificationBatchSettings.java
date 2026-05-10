package friasoft.gn.schoolapp.entity.school;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification_batch_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationBatchSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private Long tenantId;

    @Column(name = "evaluation_reminder_days_before", nullable = false)
    private Integer evaluationReminderDaysBefore = 3;

    @Column(name = "evaluation_reminder_enabled", nullable = false)
    private Boolean evaluationReminderEnabled = true;

    @Column(name = "payment_reminder_enabled", nullable = false)
    private Boolean paymentReminderEnabled = true;

    @Column(name = "timetable_change_enabled", nullable = false)
    private Boolean timetableChangeEnabled = true;

    @Column(name = "batch_chunk_size", nullable = false)
    private Integer batchChunkSize = 50;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    private Boolean smsEnabled = false;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    @PrePersist
    void touch() {
        updatedAt = Instant.now();
    }
}
