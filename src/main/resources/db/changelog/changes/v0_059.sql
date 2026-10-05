--liquibase formatted sql
--changeset friasoft:059-student-tutor-as-parent
--comment: Tuteur = fiche Parent (tutor_id) ; reprise des champs tutor_* textuels puis suppression.

ALTER TABLE schools.students
    ADD COLUMN IF NOT EXISTS tutor_id BIGINT REFERENCES schools.parents(id);

CREATE INDEX IF NOT EXISTS idx_students_tutor_id ON schools.students(tutor_id);

-- Rattache un parent existant si le téléphone tuteur correspond (même tenant).
UPDATE schools.students s
SET tutor_id = p.id
FROM schools.parents p
WHERE s.tutor_id IS NULL
  AND s.tutor_phone IS NOT NULL
  AND trim(s.tutor_phone) <> ''
  AND p.tenant_id = s.tenant_id
  AND p.phone = trim(s.tutor_phone);

-- Staging des élèves encore sans tutor_id mais avec infos tuteur textuelles.
CREATE TABLE IF NOT EXISTS schools._mig_tutor_059 (
    student_id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(40),
    email VARCHAR(180),
    profession VARCHAR(150)
);

DELETE FROM schools._mig_tutor_059;

INSERT INTO schools._mig_tutor_059 (student_id, tenant_id, first_name, last_name, phone, email, profession)
SELECT
    s.id,
    s.tenant_id,
    CASE
        WHEN NULLIF(trim(s.tutor_name), '') IS NULL THEN 'Tuteur'
        WHEN position(' ' IN trim(s.tutor_name)) > 0 THEN
            COALESCE(
                NULLIF(left(trim(substring(trim(s.tutor_name) FROM 1 FOR position(' ' IN trim(s.tutor_name)) - 1)), 100), ''),
                '—'
            )
        ELSE '—'
    END,
    CASE
        WHEN NULLIF(trim(s.tutor_name), '') IS NULL THEN 'Ancien'
        WHEN position(' ' IN trim(s.tutor_name)) > 0 THEN
            COALESCE(
                NULLIF(left(trim(substring(trim(s.tutor_name) FROM position(' ' IN trim(s.tutor_name)) + 1)), 100), ''),
                NULLIF(left(trim(substring(trim(s.tutor_name) FROM 1 FOR position(' ' IN trim(s.tutor_name)) - 1)), 100), ''),
                'Ancien'
            )
        ELSE left(trim(s.tutor_name), 100)
    END,
    NULLIF(trim(s.tutor_phone), ''),
    NULLIF(trim(s.tutor_email), ''),
    NULLIF(trim(s.tutor_profession), '')
FROM schools.students s
WHERE s.tutor_id IS NULL
  AND (
      NULLIF(trim(s.tutor_name), '') IS NOT NULL
      OR NULLIF(trim(s.tutor_phone), '') IS NOT NULL
      OR NULLIF(trim(s.tutor_email), '') IS NOT NULL
      OR NULLIF(trim(s.tutor_profession), '') IS NOT NULL
  );

-- Sécurité : first_name / last_name jamais null ni vides.
UPDATE schools._mig_tutor_059
SET first_name = '—'
WHERE first_name IS NULL OR trim(first_name) = '';

UPDATE schools._mig_tutor_059
SET last_name = 'Ancien'
WHERE last_name IS NULL OR trim(last_name) = '';

-- Crée une fiche Parent par élève (marqueur temporaire dans address pour le rattachement).
INSERT INTO schools.parents (tenant_id, last_name, first_name, phone, email, profession, address)
SELECT
    m.tenant_id,
    m.last_name,
    m.first_name,
    m.phone,
    m.email,
    m.profession,
    '__mig_tutor_059:' || m.student_id::text
FROM schools._mig_tutor_059 m
WHERE NOT EXISTS (
    SELECT 1
    FROM schools.parents p
    WHERE p.address = '__mig_tutor_059:' || m.student_id::text
);

UPDATE schools.students s
SET tutor_id = p.id
FROM schools.parents p
WHERE s.tutor_id IS NULL
  AND p.address = '__mig_tutor_059:' || s.id::text;

UPDATE schools.parents
SET address = NULL
WHERE address LIKE '__mig_tutor_059:%';

DROP TABLE IF EXISTS schools._mig_tutor_059;

ALTER TABLE schools.students DROP COLUMN IF EXISTS tutor_name;
ALTER TABLE schools.students DROP COLUMN IF EXISTS tutor_profession;
ALTER TABLE schools.students DROP COLUMN IF EXISTS tutor_phone;
ALTER TABLE schools.students DROP COLUMN IF EXISTS tutor_email;
