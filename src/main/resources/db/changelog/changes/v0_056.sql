--liquibase formatted sql
--changeset friasoft:056-subject-level-groups-and-class-stream
--comment: Liaison matières ↔ groupes de cycle ; filière lycée (SE/SM/SS) sur school_classes.

-- —— Matières × groupes de cycle ——
CREATE TABLE IF NOT EXISTS schools.subject_level_groups (
    subject_id BIGINT NOT NULL REFERENCES schools.subjects(id) ON DELETE CASCADE,
    class_level_group_id BIGINT NOT NULL REFERENCES schools.class_level_groups(id) ON DELETE CASCADE,
    PRIMARY KEY (subject_id, class_level_group_id)
);

CREATE INDEX IF NOT EXISTS idx_subject_level_groups_group
    ON schools.subject_level_groups (class_level_group_id);

-- Enrichissement référentiel global (idempotent)
INSERT INTO schools.subjects (code, name)
SELECT v.code, v.name
FROM (VALUES
    ('ECM', 'Éducation civique et morale'),
    ('EPS', 'Éducation physique et sportive'),
    ('SCI_OBS', 'Sciences d''observation')
) AS v(code, name)
WHERE NOT EXISTS (
    SELECT 1 FROM schools.subjects s
    WHERE s.school_id IS NULL AND lower(btrim(s.code)) = lower(v.code)
);

-- Périmètres par code (référentiel global uniquement)
-- FR, MATH : maternelle → lycée
INSERT INTO schools.subject_level_groups (subject_id, class_level_group_id)
SELECT s.id, g.id
FROM schools.subjects s
CROSS JOIN schools.class_level_groups g
WHERE s.school_id IS NULL
  AND lower(btrim(s.code)) IN ('fr', 'math')
  AND g.code IN ('MAT', 'PRI', 'COL', 'LYC')
  AND NOT EXISTS (
      SELECT 1 FROM schools.subject_level_groups x
      WHERE x.subject_id = s.id AND x.class_level_group_id = g.id
  );

-- EPS : maternelle → lycée
INSERT INTO schools.subject_level_groups (subject_id, class_level_group_id)
SELECT s.id, g.id
FROM schools.subjects s
CROSS JOIN schools.class_level_groups g
WHERE s.school_id IS NULL
  AND lower(btrim(s.code)) = 'eps'
  AND g.code IN ('MAT', 'PRI', 'COL', 'LYC')
  AND NOT EXISTS (
      SELECT 1 FROM schools.subject_level_groups x
      WHERE x.subject_id = s.id AND x.class_level_group_id = g.id
  );

-- HIST, ECM : primaire → lycée
INSERT INTO schools.subject_level_groups (subject_id, class_level_group_id)
SELECT s.id, g.id
FROM schools.subjects s
CROSS JOIN schools.class_level_groups g
WHERE s.school_id IS NULL
  AND lower(btrim(s.code)) IN ('hist', 'ecm')
  AND g.code IN ('PRI', 'COL', 'LYC')
  AND NOT EXISTS (
      SELECT 1 FROM schools.subject_level_groups x
      WHERE x.subject_id = s.id AND x.class_level_group_id = g.id
  );

-- SCI_OBS : primaire seulement
INSERT INTO schools.subject_level_groups (subject_id, class_level_group_id)
SELECT s.id, g.id
FROM schools.subjects s
CROSS JOIN schools.class_level_groups g
WHERE s.school_id IS NULL
  AND lower(btrim(s.code)) = 'sci_obs'
  AND g.code = 'PRI'
  AND NOT EXISTS (
      SELECT 1 FROM schools.subject_level_groups x
      WHERE x.subject_id = s.id AND x.class_level_group_id = g.id
  );

-- ANG, SVT, PC : collège + lycée
INSERT INTO schools.subject_level_groups (subject_id, class_level_group_id)
SELECT s.id, g.id
FROM schools.subjects s
CROSS JOIN schools.class_level_groups g
WHERE s.school_id IS NULL
  AND lower(btrim(s.code)) IN ('ang', 'svt', 'pc')
  AND g.code IN ('COL', 'LYC')
  AND NOT EXISTS (
      SELECT 1 FROM schools.subject_level_groups x
      WHERE x.subject_id = s.id AND x.class_level_group_id = g.id
  );

-- Matières sans périmètre (ex. créées par une école) : tous les groupes → restent assignables
INSERT INTO schools.subject_level_groups (subject_id, class_level_group_id)
SELECT s.id, g.id
FROM schools.subjects s
CROSS JOIN schools.class_level_groups g
WHERE NOT EXISTS (
    SELECT 1 FROM schools.subject_level_groups x WHERE x.subject_id = s.id
)
AND NOT EXISTS (
    SELECT 1 FROM schools.subject_level_groups x
    WHERE x.subject_id = s.id AND x.class_level_group_id = g.id
);

-- —— Filière lycée sur la classe ——
ALTER TABLE schools.school_classes
    ADD COLUMN IF NOT EXISTS stream VARCHAR(10);

ALTER TABLE schools.school_classes
    DROP CONSTRAINT IF EXISTS chk_school_classes_stream;

ALTER TABLE schools.school_classes
    ADD CONSTRAINT chk_school_classes_stream
    CHECK (stream IS NULL OR stream IN ('SE', 'SM', 'SS'));

COMMENT ON COLUMN schools.school_classes.stream IS
    'Filière lycée (SE/SM/SS). Obligatoire si niveau groupe LYC ; NULL sinon.';

-- Classes lycée déjà en base : filière par défaut SE (à ajuster ensuite côté UI)
UPDATE schools.school_classes sc
SET stream = 'SE'
FROM schools.class_levels lv
JOIN schools.class_level_groups g ON g.id = lv.group_id
WHERE sc.level_id = lv.id
  AND g.code = 'LYC'
  AND sc.stream IS NULL;
