--liquibase formatted sql
--changeset friasoft:046-preschool-garderie
--comment: Groupe Pré scolaire (avant Maternelle) avec niveau Garderie.

INSERT INTO schools.class_level_groups (code, name)
SELECT 'PRE', 'Pré scolaire'
WHERE NOT EXISTS (
  SELECT 1 FROM schools.class_level_groups WHERE code = 'PRE'
);

INSERT INTO schools.class_levels (code, name, group_id)
SELECT 'GAR', 'Garderie', g.id
FROM schools.class_level_groups g
WHERE g.code = 'PRE'
  AND NOT EXISTS (SELECT 1 FROM schools.class_levels WHERE code = 'GAR');
