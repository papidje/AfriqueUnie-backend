--liquibase formatted sql

--changeset friasoft:036-jwts-active-school-context
--comment: Mémorise l’établissement actif de la session JWT pour préserver le contexte au refresh

ALTER TABLE schools.jwts
    ADD COLUMN IF NOT EXISTS active_school_id BIGINT REFERENCES schools.schools(id) ON DELETE SET NULL;
