--liquibase formatted sql
--changeset friasoft:007-teacher-role-class-subject-teacher
--comment: Rôle TEACHER ; professeur optionnel sur class_subjects.

ALTER TABLE schools.users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE schools.users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('SUPER_ADMIN', 'ADMIN_ECOLE', 'STAFF', 'TEACHER'));

ALTER TABLE schools.class_subjects
    ADD COLUMN IF NOT EXISTS teacher_id BIGINT REFERENCES schools.users(id);

CREATE INDEX IF NOT EXISTS idx_class_subjects_teacher_id ON schools.class_subjects(teacher_id);
