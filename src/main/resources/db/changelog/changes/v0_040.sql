--liquibase formatted sql

--changeset friasoft:v0_040-notifications
CREATE TABLE schools.notifications (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    link_id BIGINT NULL,
    target_user_id BIGINT NULL REFERENCES schools.users (id) ON DELETE CASCADE,
    target_school_id BIGINT NULL REFERENCES schools.schools (id) ON DELETE CASCADE,
    target_tenant_id BIGINT NULL REFERENCES schools.tenants (id) ON DELETE CASCADE,
    CONSTRAINT chk_notification_at_least_one_target CHECK (
        target_user_id IS NOT NULL OR target_school_id IS NOT NULL OR target_tenant_id IS NOT NULL
    )
);

CREATE INDEX idx_notifications_target_user ON schools.notifications (target_user_id);
CREATE INDEX idx_notifications_target_school ON schools.notifications (target_school_id);
CREATE INDEX idx_notifications_target_tenant ON schools.notifications (target_tenant_id);
CREATE INDEX idx_notifications_link ON schools.notifications (link_id);

CREATE TABLE schools.notification_read_states (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES schools.notifications (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES schools.users (id) ON DELETE CASCADE,
    read_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_notification_read_notification_user UNIQUE (notification_id, user_id)
);

CREATE INDEX idx_notification_read_user ON schools.notification_read_states (user_id);
