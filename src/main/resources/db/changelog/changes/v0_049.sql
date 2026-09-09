--liquibase formatted sql
--changeset friasoft:049-student-account-tuition-payable-percent
--comment: Pourcentage de scolarité à payer (100 = complet, 0 = exempté). Verrouillé après 1er paiement SCOLARITE.

ALTER TABLE schools.student_accounts
    ADD COLUMN IF NOT EXISTS tuition_payable_percent DOUBLE PRECISION NOT NULL DEFAULT 100;
