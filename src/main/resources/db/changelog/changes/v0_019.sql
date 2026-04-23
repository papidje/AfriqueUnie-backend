--liquibase formatted sql
--changeset friasoft:019-payment-type-fournitures
--comment: Type de paiement FOURNITURES pour tracer les encaissements fournitures dans student_payments.

ALTER TABLE schools.student_payments DROP CONSTRAINT IF EXISTS chk_student_payments_type;

ALTER TABLE schools.student_payments
    ADD CONSTRAINT chk_student_payments_type
        CHECK (payment_type IN ('INSCRIPTION', 'REINSCRIPTION', 'SCOLARITE', 'FOURNITURES'));
