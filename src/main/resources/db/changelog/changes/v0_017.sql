--liquibase formatted sql
--changeset friasoft:017-user-role-accountant
--comment: Rôle comptable (ROLE_ACCOUNTANT) pour le personnel rattaché à une école.

ALTER TABLE schools.users DROP CONSTRAINT IF EXISTS chk_users_role;

ALTER TABLE schools.users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('SUPER_ADMIN', 'ADMIN_ECOLE', 'STAFF', 'TEACHER', 'DIRECTOR', 'ACCOUNTANT'));
