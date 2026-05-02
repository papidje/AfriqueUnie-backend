--liquibase formatted sql
--changeset friasoft:027-school-class-period-type-grading-periods
--comment: Type de période (trimestre / semestre) sur les classes + périodes de notation générées.

ALTER TABLE schools.school_classes
    ADD COLUMN period_type VARCHAR(20) NOT NULL DEFAULT 'TRIMESTER';

CREATE TABLE schools.grading_periods (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    school_class_id BIGINT NOT NULL REFERENCES schools.school_classes(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL
);

CREATE INDEX idx_grading_periods_school_class ON schools.grading_periods(school_class_id);
CREATE INDEX idx_grading_periods_tenant_id ON schools.grading_periods(tenant_id);
