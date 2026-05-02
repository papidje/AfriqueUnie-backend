--liquibase formatted sql
--changeset friasoft:022-payment-tuition-month
--comment: Mois de scolarité associé à chaque ligne SCOLARITE (reçus / historique / calcul).

ALTER TABLE schools.student_payments
    ADD COLUMN IF NOT EXISTS tuition_month_code VARCHAR(16);
