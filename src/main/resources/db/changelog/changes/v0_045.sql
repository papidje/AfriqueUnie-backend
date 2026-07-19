--liquibase formatted sql
--changeset friasoft:045-guinea-class-levels
--comment: Nomenclature primaire/secondaire guinéenne (CP1–CM2, 7e–10e, 11e–Tle). Maternelle (MAT/PS/MS/GS) inchangée.

-- Groupes — maternelle (MAT) conservée telle quelle
UPDATE schools.class_level_groups SET name = 'Primaire' WHERE code = 'PRI';
UPDATE schools.class_level_groups SET name = 'Collège (1er cycle)' WHERE code = 'COL';
UPDATE schools.class_level_groups SET name = 'Lycée (2e cycle)' WHERE code = 'LYC';

-- Maternelle : aucune modification (MAT — PS, MS, GS)

-- Primaire (6 ans) — CP → CP1, ajout CP2
UPDATE schools.class_levels SET code = 'CP1', name = '1ère année (CP1)' WHERE code = 'CP';
INSERT INTO schools.class_levels (code, name, group_id)
SELECT 'CP2', '2ème année (CP2)', g.id
FROM schools.class_level_groups g
WHERE g.code = 'PRI'
  AND NOT EXISTS (SELECT 1 FROM schools.class_levels WHERE code = 'CP2');

UPDATE schools.class_levels SET name = '3ème année (CE1)' WHERE code = 'CE1';
UPDATE schools.class_levels SET name = '4ème année (CE2)' WHERE code = 'CE2';
UPDATE schools.class_levels SET name = '5ème année (CM1)' WHERE code = 'CM1';
UPDATE schools.class_levels SET name = '6ème année (CM2)' WHERE code = 'CM2';

-- Collège guinéen (7e–10e) — mise à jour in place, IDs conservés (school_classes, fee_structures)
UPDATE schools.class_levels SET code = '7E', name = '7ème année' WHERE code = '6E';
UPDATE schools.class_levels SET code = '8E', name = '8ème année' WHERE code = '5E';
UPDATE schools.class_levels SET code = '9E', name = '9ème année' WHERE code = '4E';
UPDATE schools.class_levels SET code = '10E', name = '10ème année' WHERE code = '3E';

-- Lycée guinéen (11e–Terminale) — mise à jour in place, IDs conservés
UPDATE schools.class_levels SET code = '11E', name = '11ème année' WHERE code = '2ND';
UPDATE schools.class_levels SET code = '12E', name = '12ème année' WHERE code = '1ERE';
UPDATE schools.class_levels SET name = 'Terminale' WHERE code = 'TLE';
