--liquibase formatted sql
--changeset friasoft:013-student-account-school-year-and-payment-types
--comment: Lie student_accounts a l'annee scolaire et etend les types de paiements.

ALTER TABLE schools.student_accounts
    ADD COLUMN IF NOT EXISTS school_year_id BIGINT;

UPDATE schools.student_accounts sa
SET school_year_id = sc.year_id
FROM schools.students s
JOIN schools.school_classes sc ON sc.id = s.school_class_id
WHERE sa.student_id = s.id
  AND sa.school_year_id IS NULL;

ALTER TABLE schools.student_accounts
    ALTER COLUMN school_year_id SET NOT NULL;

ALTER TABLE schools.student_accounts
    ADD CONSTRAINT fk_student_accounts_school_year
        FOREIGN KEY (school_year_id) REFERENCES schools.school_years(id);

ALTER TABLE schools.student_accounts
    DROP CONSTRAINT IF EXISTS uq_student_accounts_student;

ALTER TABLE schools.student_accounts
    ADD CONSTRAINT uq_student_accounts_student_year UNIQUE (student_id, school_year_id);

CREATE INDEX IF NOT EXISTS idx_student_accounts_school_year_id ON schools.student_accounts(school_year_id);

ALTER TABLE schools.student_payments
    DROP CONSTRAINT IF EXISTS chk_student_payments_type;

ALTER TABLE schools.student_payments
    ADD CONSTRAINT chk_student_payments_type
        CHECK (payment_type IN ('INSCRIPTION', 'REINSCRIPTION', 'SCOLARITE'));

