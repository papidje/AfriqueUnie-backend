--liquibase formatted sql
--changeset friasoft:044-user-profile-fields
--comment: Profil utilisateur étendu — prénom, nom, naissance, sexe, téléphone, biographie ; reprise de fullname dans last_name.

ALTER TABLE schools.users ADD COLUMN first_name VARCHAR(255);
ALTER TABLE schools.users ADD COLUMN last_name VARCHAR(255);
ALTER TABLE schools.users ADD COLUMN birth_date DATE;
ALTER TABLE schools.users ADD COLUMN gender VARCHAR(50);
ALTER TABLE schools.users ADD COLUMN phone VARCHAR(100);
ALTER TABLE schools.users ADD COLUMN biography TEXT;

UPDATE schools.users
SET last_name = TRIM(fullname)
WHERE fullname IS NOT NULL
  AND TRIM(fullname) <> ''
  AND last_name IS NULL;
