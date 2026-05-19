package friasoft.gn.schoolapp.entity.notification;

import friasoft.gn.schoolapp.entity.auth.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "notification_read_states",
    uniqueConstraints = @UniqueConstraint(name = "uk_notification_read_notification_user", columnNames = {"notification_id", "user_id"})
)
public class NotificationReadState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private NotificationEntity notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt = LocalDateTime.now();

    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    /** Dernière modification de l’état (création, accept/refus, fermeture…). */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** Faux lorsque l’utilisateur a masqué la notification depuis le centre. */
    @Column(name = "is_visible", nullable = false)
    private boolean visible = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "closure_reason", length = 32)
    private NotificationClosureReason closureReason;
}
