--liquibase formatted sql
--changeset friasoft:030-student-grading-snapshots
--comment: Snapshots périodiques (moyennes, rang) calculés en batch.

CREATE TABLE schools.student_grading_snapshots (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    student_id BIGINT NOT NULL REFERENCES schools.students (id) ON DELETE CASCADE,
    school_class_id BIGINT NOT NULL REFERENCES schools.school_classes (id) ON DELETE CASCADE,
    grading_period_id BIGINT NOT NULL REFERENCES schools.grading_periods (id) ON DELETE CASCADE,
    grading_period_name VARCHAR(100) NOT NULL,
    subject_averages JSONB NOT NULL,
    period_general_average DOUBLE PRECISION,
    rank_in_class INT,
    total_evaluations INT NOT NULL DEFAULT 0,
    composition_weight DOUBLE PRECISION NOT NULL,
    last_updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_student_grading_snapshot UNIQUE (student_id, grading_period_id)
);

CREATE INDEX idx_sgs_class_period ON schools.student_grading_snapshots (school_class_id, grading_period_id);
CREATE INDEX idx_sgs_tenant ON schools.student_grading_snapshots (tenant_id);
