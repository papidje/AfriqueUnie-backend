--liquibase formatted sql
--changeset friasoft:052-cities-referential
--comment: Référentiel régions + villes (Guinée) et rattachement école.

CREATE TABLE IF NOT EXISTS schools.regions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_regions_code UNIQUE (code)
);

CREATE INDEX IF NOT EXISTS idx_regions_active ON schools.regions (active);
CREATE INDEX IF NOT EXISTS idx_regions_name ON schools.regions (name);

CREATE TABLE IF NOT EXISTS schools.cities (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    region_id BIGINT NOT NULL REFERENCES schools.regions(id),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_cities_code UNIQUE (code)
);

CREATE INDEX IF NOT EXISTS idx_cities_active ON schools.cities (active);
CREATE INDEX IF NOT EXISTS idx_cities_name ON schools.cities (name);
CREATE INDEX IF NOT EXISTS idx_cities_region_id ON schools.cities (region_id);

ALTER TABLE schools.schools
    ADD COLUMN IF NOT EXISTS city_id BIGINT REFERENCES schools.cities(id);

CREATE INDEX IF NOT EXISTS idx_schools_city_id ON schools.schools (city_id);

-- 8 régions administratives de Guinée
INSERT INTO schools.regions (code, name, active)
SELECT v.code, v.name, TRUE
FROM (VALUES
    ('CON', 'Conakry'),
    ('KIN', 'Kindia'),
    ('BOK', 'Boké'),
    ('MAM', 'Mamou'),
    ('LAB', 'Labé'),
    ('FAR', 'Faranah'),
    ('KAN', 'Kankan'),
    ('NZE', 'N''Zérékoré')
) AS v(code, name)
WHERE NOT EXISTS (SELECT 1 FROM schools.regions r WHERE r.code = v.code);

-- Préfectures / principales villes (coordonnées approx. centres)
INSERT INTO schools.cities (code, name, region_id, latitude, longitude, active)
SELECT v.code, v.name, r.id, v.lat, v.lng, TRUE
FROM (VALUES
    ('CKY', 'Conakry', 'CON', 9.6412, -13.5784),
    ('COY', 'Coyah', 'KIN', 9.7064, -13.3769),
    ('DUB', 'Dubréka', 'KIN', 9.7833, -13.5167),
    ('FOR', 'Forécariah', 'KIN', 9.4333, -13.1000),
    ('KIN', 'Kindia', 'KIN', 10.0570, -12.8658),
    ('TEL', 'Télimélé', 'KIN', 10.9000, -13.0333),
    ('BOK', 'Boké', 'BOK', 10.9400, -14.2900),
    ('BOF', 'Boffa', 'BOK', 10.1833, -14.0333),
    ('FRI', 'Fria', 'BOK', 10.3667, -13.5833),
    ('GAO', 'Gaoual', 'BOK', 11.7500, -13.2000),
    ('KOU', 'Koundara', 'BOK', 12.4833, -13.3000),
    ('MAM', 'Mamou', 'MAM', 10.3755, -12.0914),
    ('DAL', 'Dalaba', 'MAM', 10.7000, -12.2500),
    ('PIT', 'Pita', 'MAM', 11.0667, -12.4000),
    ('LAB', 'Labé', 'LAB', 11.3182, -12.2887),
    ('KOB', 'Koubia', 'LAB', 11.5833, -11.9000),
    ('LEL', 'Lélouma', 'LAB', 11.4167, -12.6833),
    ('MAL', 'Mali', 'LAB', 12.0833, -12.3000),
    ('TOU', 'Tougué', 'LAB', 11.4500, -11.6833),
    ('FAR', 'Faranah', 'FAR', 10.0404, -10.7434),
    ('DIN', 'Dinguiraye', 'FAR', 11.3000, -10.7167),
    ('KIS', 'Kissidougou', 'FAR', 9.1900, -10.1000),
    ('KAN', 'Kankan', 'KAN', 10.3854, -9.3057),
    ('KER', 'Kérouané', 'KAN', 9.2667, -9.0167),
    ('KRS', 'Kouroussa', 'KAN', 10.6500, -9.8833),
    ('MAN', 'Mandiana', 'KAN', 10.6333, -8.7000),
    ('SIG', 'Siguiri', 'KAN', 11.4228, -9.1685),
    ('GUE', 'Guéckédou', 'NZE', 8.5674, -10.1336),
    ('MAC', 'Macenta', 'NZE', 8.5500, -9.4667),
    ('NZE', 'N''Zérékoré', 'NZE', 7.7562, -8.8179),
    ('YOM', 'Yomou', 'NZE', 7.5667, -9.2500),
    ('BEY', 'Beyla', 'NZE', 8.6833, -8.6500),
    ('LOL', 'Lola', 'NZE', 7.8000, -8.5333)
) AS v(code, name, region_code, lat, lng)
JOIN schools.regions r ON r.code = v.region_code
WHERE NOT EXISTS (SELECT 1 FROM schools.cities c WHERE c.code = v.code);
