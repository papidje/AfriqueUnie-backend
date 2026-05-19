package friasoft.gn.schoolapp.entity.notification;

import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.tenant.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "link_id")
    private Long linkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_school_id")
    private School targetSchool;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_tenant_id")
    private Tenant targetTenant;
}
