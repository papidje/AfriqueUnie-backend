--liquibase formatted sql
--changeset friasoft:033-notification-history-body-content
--comment: Historique notifications — stockage du contenu consultable (texte intégral).

ALTER TABLE schools.notification_delivery_history
    ADD COLUMN IF NOT EXISTS body_content TEXT;
