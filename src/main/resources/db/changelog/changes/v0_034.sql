--liquibase formatted sql
--changeset friasoft:034-notification-school-scope
--comment: Notifications — périmètre école (school_id), dédoublonnage et historique par établissement.

ALTER TABLE schools.notification_logs DROP CONSTRAINT IF EXISTS uq_notification_logs_dedup;

ALTER TABLE schools.notification_logs
    ADD COLUMN IF NOT EXISTS school_id BIGINT REFERENCES schools.schools(id) ON DELETE CASCADE;

ALTER TABLE schools.notification_delivery_history
    ADD COLUMN IF NOT EXISTS school_id BIGINT REFERENCES schools.schools(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_logs_dedup_school
    ON schools.notification_logs (school_id, event_type, reference_id, parent_id)
    WHERE school_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_notification_logs_school_sent ON schools.notification_logs(school_id, sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_history_school_created ON schools.notification_delivery_history(school_id, created_at DESC);
