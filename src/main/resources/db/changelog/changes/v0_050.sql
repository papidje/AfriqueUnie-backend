--liquibase formatted sql
--changeset friasoft:050-student-matricule-unique-card-number
--comment: Unicité matricule, numéro de carte élève (modifiable).

-- Dédupliquer d’éventuels matricules en double avant l’index unique.
UPDATE schools.students s
SET matricule = s.matricule || '-' || s.id::text
WHERE s.id IN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY matricule ORDER BY id) AS rn
        FROM schools.students
        WHERE matricule IS NOT NULL
    ) d
    WHERE d.rn > 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_students_matricule
    ON schools.students (matricule);

ALTER TABLE schools.students
    ADD COLUMN IF NOT EXISTS card_number VARCHAR(50) NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_students_tenant_card_number
    ON schools.students (tenant_id, card_number)
    WHERE card_number IS NOT NULL AND btrim(card_number) <> '';
