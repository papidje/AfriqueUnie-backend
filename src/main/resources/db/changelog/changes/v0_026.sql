--liquibase formatted sql
--changeset friasoft:026-subject-school-id
--comment: Matières globales (school_id NULL) ou propres à une école ; unicité du code par portée.

ALTER TABLE schools.subjects
    ADD COLUMN school_id BIGINT REFERENCES schools.schools(id) ON DELETE CASCADE;

ALTER TABLE schools.subjects DROP CONSTRAINT IF EXISTS subjects_code_key;

CREATE UNIQUE INDEX uq_subjects_code_global
    ON schools.subjects (lower(btrim(code::text)))
    WHERE school_id IS NULL;

CREATE UNIQUE INDEX uq_subjects_school_code
    ON schools.subjects (school_id, lower(btrim(code::text)))
    WHERE school_id IS NOT NULL;
