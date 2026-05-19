--liquibase formatted sql

--changeset friasoft:v0_042-notification-read-state-closure-reason
ALTER TABLE schools.notification_read_states ADD COLUMN closure_reason VARCHAR(32) NULL;
