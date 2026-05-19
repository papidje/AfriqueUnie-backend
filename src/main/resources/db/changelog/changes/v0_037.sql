--liquibase formatted sql

--changeset friasoft:037-merge-accountant-into-staff
--comment: Fusion du rôle ACCOUNTANT dans STAFF (données + contrainte CHECK sur users.role).

UPDATE schools.users SET role = 'STAFF' WHERE role = 'ACCOUNTANT';

UPDATE schools.user_school_affiliations SET role = 'STAFF' WHERE role = 'ACCOUNTANT';

ALTER TABLE schools.users DROP CONSTRAINT IF EXISTS chk_users_role;

ALTER TABLE schools.users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('SUPER_ADMIN', 'ADMIN_ECOLE', 'STAFF', 'TEACHER', 'DIRECTOR'));
