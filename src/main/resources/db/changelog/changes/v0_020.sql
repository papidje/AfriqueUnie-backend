--liquibase formatted sql
--changeset friasoft:020-school-class-capacity
--comment: Capacité d’accueil par classe (effectif max) pour l’affichage inscription / capacité.

ALTER TABLE schools.school_classes
    ADD COLUMN IF NOT EXISTS capacity integer NOT NULL DEFAULT 40;

COMMENT ON COLUMN schools.school_classes.capacity IS 'Nombre maximum d’élèves prévus pour la classe.';
