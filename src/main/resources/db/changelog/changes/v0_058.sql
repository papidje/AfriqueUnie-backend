--liquibase formatted sql
--changeset friasoft:058-parents-phone-nullable
--comment: Téléphone parent optionnel (inscription allégée / dossier progressif).

ALTER TABLE schools.parents ALTER COLUMN phone DROP NOT NULL;
