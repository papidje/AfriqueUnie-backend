--liquibase formatted sql

--changeset friasoft:038-users-email-unique-ci
--comment: Unicité de l'e-mail (insensible à la casse, espaces bout de chaîne ignorés). Garde-fou DB si une création de compte est tentée pour un e-mail déjà présent. Échec de migration si des doublons existent déjà — les fusionner ou corriger les lignes avant d'appliquer.

-- Index unique partiel : plusieurs lignes avec email NULL restent possibles (legacy / données incomplètes).
CREATE UNIQUE INDEX uk_users_email_lower ON schools.users (lower(btrim(email)))
WHERE email IS NOT NULL AND btrim(email) <> '';
