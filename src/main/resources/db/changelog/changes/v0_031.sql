--liquibase formatted sql
--changeset friasoft:031-evaluations-coeff-from-class-subject
--comment: Aligner evaluations.coefficient sur class_subjects.coefficient (copie pour requêtes / rapports).

UPDATE schools.evaluations e
SET coefficient = COALESCE(cs.coefficient::double precision, 1.0)
FROM schools.class_subjects cs
WHERE e.class_subject_id = cs.id;
