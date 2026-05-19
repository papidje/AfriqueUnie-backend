package friasoft.gn.schoolapp.entity.notification;

/**
 * Comment la notification a été fermée / lue côté utilisateur (hors simple présence d’un état lu).
 */
public enum NotificationClosureReason {
    /** Marquée lue en masse ou équivalent — pas une décision d’invitation. */
    MARK_READ,
    /** Masquée depuis le centre de notifications. */
    DISMISSED,
    INVITATION_ACCEPTED,
    INVITATION_REFUSED
}
