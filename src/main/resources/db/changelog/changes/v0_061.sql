--liquibase formatted sql
--changeset friasoft:061-class-level-sort-order
--comment: Ordre d’affichage administrable pour cycles (class_level_groups) et niveaux (class_levels).

ALTER TABLE schools.class_level_groups
    ADD COLUMN IF NOT EXISTS sort_order INTEGER;

UPDATE schools.class_level_groups
SET sort_order = CASE code
    WHEN 'PRE' THEN 1
    WHEN 'MAT' THEN 2
    WHEN 'PRI' THEN 3
    WHEN 'COL' THEN 4
    WHEN 'LYC' THEN 5
    ELSE COALESCE(sort_order, 100 + id::int)
END
WHERE sort_order IS NULL;

ALTER TABLE schools.class_level_groups
    ALTER COLUMN sort_order SET NOT NULL;

ALTER TABLE schools.class_levels
    ADD COLUMN IF NOT EXISTS sort_order INTEGER;

UPDATE schools.class_levels
SET sort_order = CASE code
    WHEN 'GAR' THEN 10
    WHEN 'PS' THEN 20
    WHEN 'MS' THEN 30
    WHEN 'GS' THEN 40
    WHEN 'CP1' THEN 50
    WHEN 'CP2' THEN 60
    WHEN 'CE1' THEN 70
    WHEN 'CE2' THEN 80
    WHEN 'CM1' THEN 90
    WHEN 'CM2' THEN 100
    WHEN '7E' THEN 110
    WHEN '8E' THEN 120
    WHEN '9E' THEN 130
    WHEN '10E' THEN 140
    WHEN '11E' THEN 150
    WHEN '12E' THEN 160
    WHEN 'TLE' THEN 170
    ELSE COALESCE(sort_order, 1000 + id::int)
END
WHERE sort_order IS NULL;

ALTER TABLE schools.class_levels
    ALTER COLUMN sort_order SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_class_level_groups_sort_order
    ON schools.class_level_groups (sort_order);

CREATE INDEX IF NOT EXISTS idx_class_levels_sort_order
    ON schools.class_levels (sort_order);
