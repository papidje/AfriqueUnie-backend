package friasoft.gn.schoolapp.entity.school;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification_delivery_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDeliveryHistory {

    public enum Source {
        BATCH,
        MANUAL
    }

    public enum Status {
        SUCCESS,
        FAILURE
    }

    public enum Channel {
        EMAIL,
        SMS,
        BOTH
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "school_id")
    private Long schoolId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Source source;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "parent_id")
    private Long parentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Column(name = "recipients_summary", length = 2000)
    private String recipientsSummary;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(length = 300)
    private String title;

    @Column(name = "body_preview", length = 4000)
    private String bodyPreview;

    /** Texte intégral (sans HTML) pour consultation dans l’historique ; peut être volumineux. */
    @Column(name = "body_content", columnDefinition = "TEXT")
    private String bodyContent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
