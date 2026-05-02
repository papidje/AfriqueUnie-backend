--liquibase formatted sql
--changeset friasoft:009-fee-structure-refactor
--comment: Suppression enrollment/fee/payment et ajout de fee_structures.

DROP TRIGGER IF EXISTS trg_audit_payments ON schools.payments;
DROP TRIGGER IF EXISTS trg_audit_fees ON schools.fees;
DROP TRIGGER IF EXISTS trg_audit_enrollments ON schools.enrollments;

DROP TABLE IF EXISTS schools.payments_audit;
DROP TABLE IF EXISTS schools.fees_audit;
DROP TABLE IF EXISTS schools.enrollments_audit;

DROP TABLE IF EXISTS schools.payments;
DROP TABLE IF EXISTS schools.fees;
DROP TABLE IF EXISTS schools.enrollments;

CREATE TABLE schools.fee_structures (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES schools.tenants(id),
    class_level_id BIGINT NOT NULL REFERENCES schools.class_levels(id) ON DELETE CASCADE,
    school_year_id BIGINT NOT NULL REFERENCES schools.school_years(id) ON DELETE CASCADE,
    registration_fee DOUBLE PRECISION NOT NULL DEFAULT 0,
    re_registration_fee DOUBLE PRECISION NOT NULL DEFAULT 0,
    monthly_tuition_fee DOUBLE PRECISION NOT NULL DEFAULT 0,
    currency VARCHAR(10) NOT NULL DEFAULT 'GNF',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_fee_structures_level_year UNIQUE (class_level_id, school_year_id)
);

CREATE INDEX idx_fee_structures_tenant_id ON schools.fee_structures(tenant_id);
CREATE INDEX idx_fee_structures_school_year_id ON schools.fee_structures(school_year_id);
CREATE INDEX idx_fee_structures_class_level_id ON schools.fee_structures(class_level_id);
