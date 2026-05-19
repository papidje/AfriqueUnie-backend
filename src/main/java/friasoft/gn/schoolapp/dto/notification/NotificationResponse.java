package friasoft.gn.schoolapp.dto.notification;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    String title,
    String content,
    String type,
    LocalDateTime createdAt,
    Long linkId,
    boolean read,
    boolean processed,
    /** Dernière mise à jour de l’état (lu / traité / fermé) pour cet utilisateur. */
    LocalDateTime updatedAt,
    /** Toujours true dans les listes renvoyées (les masquées sont exclues). */
    boolean visible,
    /** Raison de fermeture pour cet utilisateur ({@code MARK_READ}, invitation, etc.). */
    String closureReason,
    String schoolName
) {}
