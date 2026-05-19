--liquibase formatted sql

--changeset friasoft:039-user-platform-roles-ddl
--comment: Rôles plateforme (SUPER_ADMIN, ADMIN_ECOLE) hors colonne users.role

CREATE TABLE schools.user_platform_roles (
    user_id BIGINT PRIMARY KEY REFERENCES schools.users(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    CONSTRAINT chk_user_platform_roles_role CHECK (role IN ('SUPER_ADMIN', 'ADMIN_ECOLE'))
);

--changeset friasoft:039-user-platform-roles-backfill
--comment: Migration depuis users.role avant suppression de la colonne

INSERT INTO schools.user_platform_roles (user_id, role)
SELECT id, role
FROM schools.users
WHERE role IN ('SUPER_ADMIN', 'ADMIN_ECOLE')
ON CONFLICT (user_id) DO NOTHING;

--changeset friasoft:039-affiliations-show-info-and-unique-triplet
--comment: Multi-rôles par école ; confidentialité cross-tenant

ALTER TABLE schools.user_school_affiliations
    ADD COLUMN IF NOT EXISTS show_info_to_tenant BOOLEAN NOT NULL DEFAULT false;

UPDATE schools.user_school_affiliations SET show_info_to_tenant = true WHERE is_active = true;

ALTER TABLE schools.user_school_affiliations DROP CONSTRAINT IF EXISTS unique_user_school;

ALTER TABLE schools.user_school_affiliations
    ADD CONSTRAINT unique_user_school_role UNIQUE (user_id, school_id, role);

--changeset friasoft:039-users-drop-role-column
--comment: Suppression du rôle global sur users

ALTER TABLE schools.users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE schools.users DROP COLUMN IF EXISTS role;
