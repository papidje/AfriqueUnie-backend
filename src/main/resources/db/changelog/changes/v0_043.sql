--liquibase formatted sql

--changeset friasoft:v0_043-affiliation-admin-access-suspended
ALTER TABLE schools.user_school_affiliations
ADD COLUMN admin_access_suspended BOOLEAN NOT NULL DEFAULT FALSE;
