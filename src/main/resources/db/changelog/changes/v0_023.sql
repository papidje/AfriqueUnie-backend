--liquibase formatted sql
--changeset friasoft:023-payment-validated-by-user
--comment: Utilisateur connecté ayant enregistré la ligne de paiement (affichage historique fiche élève).

ALTER TABLE schools.student_payments
    ADD COLUMN IF NOT EXISTS validated_by_user_id BIGINT REFERENCES schools.users (id);

CREATE INDEX IF NOT EXISTS idx_student_payments_validated_by_user ON schools.student_payments (validated_by_user_id);
