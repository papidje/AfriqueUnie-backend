--liquibase formatted sql
--changeset friasoft:029-evaluations-max-score
--comment: Note maximale sur une évaluation (barème), défaut 20.

ALTER TABLE schools.evaluations ADD COLUMN IF NOT EXISTS max_score DOUBLE PRECISION NOT NULL DEFAULT 20;
