--liquibase formatted sql
--changeset friasoft:015-student-payments-mode
--comment: Ajout du mode de paiement pour les encaissements.

ALTER TABLE schools.student_payments
    ADD COLUMN IF NOT EXISTS payment_mode VARCHAR(30);

ALTER TABLE schools.student_payments
    DROP CONSTRAINT IF EXISTS chk_student_payments_mode;

ALTER TABLE schools.student_payments
    ADD CONSTRAINT chk_student_payments_mode
        CHECK (payment_mode IS NULL OR payment_mode IN ('ESPECES', 'ORANGE_MONEY', 'MOOV_MONEY', 'VIREMENT'));

