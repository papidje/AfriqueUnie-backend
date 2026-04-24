--liquibase formatted sql
--changeset friasoft:025-school-theme-font
--comment: White label — thème et police par établissement

ALTER TABLE schools.schools ADD COLUMN IF NOT EXISTS theme_name VARCHAR(64) NOT NULL DEFAULT 'classique';
ALTER TABLE schools.schools ADD COLUMN IF NOT EXISTS font_name VARCHAR(64) NOT NULL DEFAULT 'inter';
