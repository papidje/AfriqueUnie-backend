--liquibase formatted sql

--changeset friasoft:035-user-school-affiliations-ddl
--comment: Table d’affiliation multi-écoles (Phase A) — contrainte unique (user_id, school_id)

CREATE TABLE schools.user_school_affiliations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES schools.users(id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools.schools(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT unique_user_school UNIQUE (user_id, school_id)
);

CREATE INDEX idx_user_school_affiliations_user_id ON schools.user_school_affiliations(user_id);
CREATE INDEX idx_user_school_affiliations_school_id ON schools.user_school_affiliations(school_id);

--changeset friasoft:035-user-school-affiliations-backfill
--comment: Copie des rattachements users.school_id vers user_school_affiliations (rétrocompat : colonnes users inchangées)

INSERT INTO schools.user_school_affiliations (user_id, school_id, role, is_active)
SELECT id, school_id, role, true
FROM schools.users
WHERE school_id IS NOT NULL;
