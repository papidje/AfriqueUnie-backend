package friasoft.gn.schoolapp.service.communication;

/**
 * Identifiants {@code reference_id} pour le dédoublonnage (unique avec {@code school_id}, {@code event_type}, {@code parent_id}).
 */
public final class NotificationDedupeRefs {

    private NotificationDedupeRefs() {
    }

    /** Évite les collisions entre classes lorsque {@code change_seq} est réinitialisé par classe. */
    public static long timetableChange(long schoolClassId, long changeSeq) {
        return schoolClassId * 1_000_000L + changeSeq;
    }
}
