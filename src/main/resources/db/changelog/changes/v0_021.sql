--liquibase formatted sql
--changeset friasoft:021-payment-receipt-reference
--comment: Référence de reçu commune aux lignes d’un encaissement + auteur saisi.

ALTER TABLE schools.student_payments
    ADD COLUMN IF NOT EXISTS receipt_reference VARCHAR(64);

ALTER TABLE schools.student_payments
    ADD COLUMN IF NOT EXISTS recorded_by VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_student_payments_receipt_ref ON schools.student_payments (receipt_reference);
