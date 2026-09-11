--liquibase formatted sql
--changeset friasoft:051-payment-reference
--comment: Référence opérateur/banque (hors espèces) sur les lignes d'encaissement.

ALTER TABLE schools.student_payments
    ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(100);
