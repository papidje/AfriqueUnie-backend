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

-- Crée une fiche Parent pour chaque élève encore sans tutor_id mais avec infos tuteur.
DO $$
DECLARE
    r RECORD;
    new_id BIGINT;
    raw_name TEXT;
    fn TEXT;
    ln TEXT;
    sp INT;
BEGIN
    FOR r IN
        SELECT id, tenant_id, tutor_name, tutor_phone, tutor_email, tutor_profession
        FROM schools.students
        WHERE tutor_id IS NULL
          AND (
              NULLIF(trim(tutor_name), '') IS NOT NULL
              OR NULLIF(trim(tutor_phone), '') IS NOT NULL
              OR NULLIF(trim(tutor_email), '') IS NOT NULL
              OR NULLIF(trim(tutor_profession), '') IS NOT NULL
          )
    LOOP
        new_id := NULL;
        IF r.tutor_phone IS NOT NULL AND trim(r.tutor_phone) <> '' THEN
            SELECT p.id INTO new_id
            FROM schools.parents p
            WHERE p.tenant_id = r.tenant_id
              AND p.phone = trim(r.tutor_phone)
            LIMIT 1;
        END IF;

        IF new_id IS NULL THEN
            raw_name := NULLIF(trim(r.tutor_name), '');
            IF raw_name IS NULL THEN
                fn := 'Tuteur';
                ln := 'Ancien';
            ELSE
                sp := position(' ' IN raw_name);
                IF sp > 0 THEN
                    fn := trim(substring(raw_name FROM 1 FOR sp - 1));
                    ln := trim(substring(raw_name FROM sp + 1));
                    IF ln = '' THEN
                        ln := fn;
                        fn := '—';
                    END IF;
                ELSE
                    fn := '—';
                    ln := raw_name;
                END IF;
            END IF;

            INSERT INTO schools.parents (tenant_id, last_name, first_name, phone, email, profession, address)
            VALUES (
                r.tenant_id,
                left(ln, 100),
                left(fn, 100),
                NULLIF(trim(r.tutor_phone), ''),
                NULLIF(trim(r.tutor_email), ''),
                NULLIF(trim(r.tutor_profession), ''),
                NULL
            )
            RETURNING id INTO new_id;
        END IF;

        UPDATE schools.students SET tutor_id = new_id WHERE id = r.id;
    END LOOP;
END $$;

ALTER TABLE schools.students DROP COLUMN IF EXISTS tutor_name;
ALTER TABLE schools.students DROP COLUMN IF EXISTS tutor_profession;
ALTER TABLE schools.students DROP COLUMN IF EXISTS tutor_phone;
ALTER TABLE schools.students DROP COLUMN IF EXISTS tutor_email;
