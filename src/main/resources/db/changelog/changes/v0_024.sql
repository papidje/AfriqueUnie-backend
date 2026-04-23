--liquibase formatted sql
--changeset friasoft:024-student-detail-extended-fields
--comment: Étend la fiche élève (coordonnées, santé, tuteur, statut, photo).

ALTER TABLE schools.students ADD COLUMN IF NOT EXISTS birth_place VARCHAR(150);
ALTER TABLE schools.students ADD COLUMN IF NOT EXISTS nationality VARCHAR(120);
ALTER TABLE schools.students ADD COLUMN IF NOT EXISTS address VARCHAR(255);
ALTER TABLE schools.students ADD COLUMN IF NOT EXISTS communication_phone VARCHAR(40);
ALTER TABLE schools.students ADD COLUMN IF NOT EXISTS communication_email VARCHAR(180);
ALTER TABLE schools.students ADD COLUMN IF NOT EXISTS blood_group VARCHAR(20);
ALTER TABLE schools.students ADD COLUMN IF NOT EXISTS allergies VARCHAR(500);
ALTER TABLE schools.students ADD COLUMN IF NOT EXISTS tutor_name VARCHAR(150);
ALTER TABLE schools.students ADD COLUMN IF NOT EXISTS tutor_profession VARCHAR(150);
ALTER TABLE schools.students ADD COLUMN IF NOT EXISTS tutor_phone VARCHAR(40);
ALTER TABLE schools.students ADD COLUMN IF NOT EXISTS tutor_email VARCHAR(180);
ALTER TABLE schools.students ADD COLUMN IF NOT EXISTS photo_path VARCHAR(255);
ALTER TABLE schools.students ADD COLUMN IF NOT EXISTS enrollment_status VARCHAR(30) DEFAULT 'INSCRIT';
ALTER TABLE schools.students ADD COLUMN IF NOT EXISTS class_history VARCHAR(255);
