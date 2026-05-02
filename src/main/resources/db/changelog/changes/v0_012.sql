--liquibase formatted sql
--changeset friasoft:012-student-account-payments
--comment: Comptabilité minimale pour inscriptions.

CREATE TABLE IF NOT EXISTS schools.student_accounts (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES schools.tenants(id),
    student_id BIGINT NOT NULL REFERENCES schools.students(id) ON DELETE CASCADE,
    currency VARCHAR(10) NOT NULL DEFAULT 'GNF',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_student_accounts_student UNIQUE (student_id)
);

CREATE INDEX IF NOT EXISTS idx_student_accounts_tenant_id ON schools.student_accounts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_student_accounts_student_id ON schools.student_accounts(student_id);

CREATE TABLE IF NOT EXISTS schools.student_payments (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES schools.tenants(id),
    student_account_id BIGINT NOT NULL REFERENCES schools.student_accounts(id) ON DELETE CASCADE,
    payment_type VARCHAR(30) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'GNF',
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_student_payments_type CHECK (payment_type IN ('INSCRIPTION'))
);

CREATE INDEX IF NOT EXISTS idx_student_payments_tenant_id ON schools.student_payments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_student_payments_account_id ON schools.student_payments(student_account_id);
CREATE INDEX IF NOT EXISTS idx_student_payments_type ON schools.student_payments(payment_type);

