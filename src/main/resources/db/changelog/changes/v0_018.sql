--liquibase formatted sql
--changeset friasoft:018-users-merge-assigned-school-into-school
--comment: Un seul lien établissement (school_id) ; suppression de assigned_school_id. Les directeurs migrent depuis assigned_school_id.

UPDATE schools.users u
SET school_id = u.assigned_school_id
WHERE u.assigned_school_id IS NOT NULL
  AND (u.school_id IS NULL OR u.role = 'DIRECTOR');

ALTER TABLE schools.users DROP CONSTRAINT IF EXISTS fk_users_assigned_school;

ALTER TABLE schools.users DROP COLUMN IF EXISTS assigned_school_id;
