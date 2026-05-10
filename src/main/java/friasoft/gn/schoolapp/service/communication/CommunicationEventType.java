package friasoft.gn.schoolapp.service.communication;

public final class CommunicationEventType {

    private CommunicationEventType() {
    }

    public static final String EVALUATION_REMINDER = "EVALUATION_REMINDER";
    public static final String PAYMENT_OVERDUE_REMINDER = "PAYMENT_OVERDUE_REMINDER";
    public static final String TIMETABLE_CHANGED = "TIMETABLE_CHANGED";
    public static final String MANUAL_URGENT = "MANUAL_URGENT";

    /** Valeur {@code parent_id} lorsque le destinataire est un tuteur sans ligne {@code parents}. */
    public static final long SYNTHETIC_PARENT_ID_TUTOR = 0L;
}
