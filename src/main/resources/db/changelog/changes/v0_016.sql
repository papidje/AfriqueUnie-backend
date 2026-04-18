--liquibase formatted sql
--changeset friasoft:016-user-assigned-school-director
--comment: École assignée pour les comptes directeur (ROLE_DIRECTOR).

ALTER TABLE schools.users
    ADD COLUMN IF NOT EXISTS assigned_school_id BIGINT;

ALTER TABLE schools.users
    DROP CONSTRAINT IF EXISTS fk_users_assigned_school;

ALTER TABLE schools.users
    ADD CONSTRAINT fk_users_assigned_school
        FOREIGN KEY (assigned_school_id) REFERENCES schools.schools (id);

ALTER TABLE schools.users DROP CONSTRAINT IF EXISTS chk_users_role;

ALTER TABLE schools.users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('SUPER_ADMIN', 'ADMIN_ECOLE', 'STAFF', 'TEACHER', 'DIRECTOR'));
