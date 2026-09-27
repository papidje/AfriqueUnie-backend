--liquibase formatted sql
--changeset friasoft:057-subject-addition-requests
--comment: Demandes d’ajout de matière au référentiel global (école → super-admin) + fil de commentaires.

CREATE TABLE IF NOT EXISTS schools.subject_addition_requests (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES schools.tenants(id),
    school_id BIGINT NOT NULL REFERENCES schools.schools(id) ON DELETE CASCADE,
    requested_by_user_id BIGINT NOT NULL REFERENCES schools.users(id),
    subject_name VARCHAR(200) NOT NULL,
    class_level_id BIGINT NOT NULL REFERENCES schools.class_levels(id),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_subject_id BIGINT REFERENCES schools.subjects(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    closed_at TIMESTAMP,
    closed_by_user_id BIGINT REFERENCES schools.users(id),
    CONSTRAINT chk_subject_addition_requests_status
        CHECK (status IN ('OPEN', 'ACCEPTED', 'REFUSED'))
);

CREATE INDEX IF NOT EXISTS idx_subject_addition_requests_school
    ON schools.subject_addition_requests (school_id, status);

CREATE INDEX IF NOT EXISTS idx_subject_addition_requests_requester
    ON schools.subject_addition_requests (requested_by_user_id, status);

CREATE INDEX IF NOT EXISTS idx_subject_addition_requests_status_created
    ON schools.subject_addition_requests (status, created_at DESC);

CREATE TABLE IF NOT EXISTS schools.subject_addition_request_comments (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL REFERENCES schools.subject_addition_requests(id) ON DELETE CASCADE,
    author_user_id BIGINT NOT NULL REFERENCES schools.users(id),
    body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subject_addition_request_comments_request
    ON schools.subject_addition_request_comments (request_id, created_at);
