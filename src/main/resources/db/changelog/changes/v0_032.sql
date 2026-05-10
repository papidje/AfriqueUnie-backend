--liquibase formatted sql
--changeset friasoft:032-notification-communication-module
--comment: Transmission d'informations — logs anti-doublon, historique d'envois, paramètres batch, état emploi du temps.

CREATE TABLE schools.notification_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    reference_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_logs_dedup UNIQUE (tenant_id, event_type, reference_id, parent_id)
);

CREATE INDEX idx_notification_logs_tenant_sent ON schools.notification_logs(tenant_id, sent_at DESC);

CREATE TABLE schools.notification_delivery_history (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    source VARCHAR(20) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    reference_id BIGINT,
    parent_id BIGINT,
    status VARCHAR(20) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipients_summary VARCHAR(2000),
    error_message VARCHAR(2000),
    title VARCHAR(300),
    body_preview VARCHAR(4000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_history_tenant_created ON schools.notification_delivery_history(tenant_id, created_at DESC);

CREATE TABLE schools.notification_batch_settings (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL UNIQUE,
    evaluation_reminder_days_before INT NOT NULL DEFAULT 3,
    evaluation_reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    payment_reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    timetable_change_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    batch_chunk_size INT NOT NULL DEFAULT 50,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP
);

CREATE TABLE schools.notification_timetable_state (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    school_class_id BIGINT NOT NULL REFERENCES schools.school_classes(id) ON DELETE CASCADE,
    fingerprint VARCHAR(256) NOT NULL,
    change_seq BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_notification_timetable_class UNIQUE (tenant_id, school_class_id)
);

CREATE INDEX idx_notification_timetable_tenant ON schools.notification_timetable_state(tenant_id);
