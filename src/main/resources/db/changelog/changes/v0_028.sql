--liquibase formatted sql
--changeset friasoft:028-evaluations-grades
--comment: Évaluations (liées à class_subject + grading_period) et notes par élève.

CREATE TABLE schools.evaluations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    class_subject_id BIGINT NOT NULL REFERENCES schools.class_subjects(id) ON DELETE CASCADE,
    grading_period_id BIGINT NOT NULL REFERENCES schools.grading_periods(id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    eval_type VARCHAR(30) NOT NULL,
    coefficient DOUBLE PRECISION NOT NULL DEFAULT 1,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE schools.grades (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    evaluation_id BIGINT NOT NULL REFERENCES schools.evaluations(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES schools.students(id) ON DELETE CASCADE,
    value DOUBLE PRECISION,
    comment VARCHAR(500),
    CONSTRAINT uq_grade_eval_student UNIQUE (evaluation_id, student_id)
);

CREATE INDEX idx_evaluations_tenant ON schools.evaluations(tenant_id);
CREATE INDEX idx_evaluations_class_subject ON schools.evaluations(class_subject_id);
CREATE INDEX idx_evaluations_grading_period ON schools.evaluations(grading_period_id);
CREATE INDEX idx_grades_tenant ON schools.grades(tenant_id);
CREATE INDEX idx_grades_evaluation ON schools.grades(evaluation_id);
CREATE INDEX idx_grades_student ON schools.grades(student_id);
