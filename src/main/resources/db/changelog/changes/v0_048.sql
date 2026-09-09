--liquibase formatted sql
--changeset friasoft:048-fee-structure-annual-tuition
--comment: Scolarité annuelle optionnelle (toggle Mensuelle/Annuelle) ; null = mode mensuel.

ALTER TABLE schools.fee_structures
    ADD COLUMN IF NOT EXISTS annual_tuition_fee DOUBLE PRECISION NULL;
