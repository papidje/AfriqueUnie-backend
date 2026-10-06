--liquibase formatted sql
--changeset friasoft:060-parent-civility
--comment: Civilité obligatoire à la saisie sur les fiches Parent (nullable pour les lignes existantes).

ALTER TABLE schools.parents
    ADD COLUMN IF NOT EXISTS civility VARCHAR(20);

ALTER TABLE schools.parents
    DROP CONSTRAINT IF EXISTS parents_civility_check;

ALTER TABLE schools.parents
    ADD CONSTRAINT parents_civility_check
    CHECK (civility IS NULL OR civility IN ('MONSIEUR', 'MADAME'));
