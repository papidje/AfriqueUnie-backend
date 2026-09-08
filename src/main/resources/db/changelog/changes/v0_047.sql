--liquibase formatted sql
--changeset friasoft:047-student-school-id
--comment: school_id sur élèves pour listes sans classe / désinscrits ; backfill depuis la classe.

ALTER TABLE schools.students
  ADD COLUMN IF NOT EXISTS school_id BIGINT NULL;

ALTER TABLE schools.students
  DROP CONSTRAINT IF EXISTS fk_students_school;

ALTER TABLE schools.students
  ADD CONSTRAINT fk_students_school
  FOREIGN KEY (school_id) REFERENCES schools.schools (id);

CREATE INDEX IF NOT EXISTS idx_students_school_id ON schools.students (school_id);

CREATE INDEX IF NOT EXISTS idx_students_school_class_null
  ON schools.students (school_id)
  WHERE school_class_id IS NULL;

UPDATE schools.students s
SET school_id = sub.school_id
FROM (
  SELECT sc.id AS class_id, y.school_id
  FROM schools.school_classes sc
  JOIN schools.school_years y ON y.id = sc.year_id
) sub
WHERE s.school_class_id = sub.class_id
  AND s.school_id IS NULL;
