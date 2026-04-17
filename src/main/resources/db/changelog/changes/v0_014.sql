--liquibase formatted sql
--changeset friasoft:014-fee-structure-supplies-and-account-flag
--comment: Ajout des frais de fournitures et du flag de paiement sur student_account.

ALTER TABLE schools.fee_structures
    ADD COLUMN IF NOT EXISTS supplies_fee DOUBLE PRECISION NOT NULL DEFAULT 0;

ALTER TABLE schools.fee_structures
    ADD COLUMN IF NOT EXISTS supplies_column_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE schools.student_accounts
    ADD COLUMN IF NOT EXISTS supplies_paid BOOLEAN NOT NULL DEFAULT FALSE;

