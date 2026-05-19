--liquibase formatted sql

--changeset friasoft:v0_041-notification-read-state-updated-visible
--comment: Date de dernière mise à jour de l’état lu / masquage par utilisateur

ALTER TABLE schools.notification_read_states ADD COLUMN updated_at TIMESTAMP;
UPDATE schools.notification_read_states SET updated_at = read_at WHERE updated_at IS NULL;
ALTER TABLE schools.notification_read_states ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE schools.notification_read_states ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE schools.notification_read_states ADD COLUMN is_visible BOOLEAN NOT NULL DEFAULT TRUE;
