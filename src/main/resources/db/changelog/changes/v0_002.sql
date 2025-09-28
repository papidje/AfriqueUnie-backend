--liquibase formatted sql
--changeset friasoft:init-school-structure

-- Années scolaires (propres à chaque école)
CREATE TABLE schools.school_years (
                                      id BIGSERIAL PRIMARY KEY,
                                      school_id BIGINT NOT NULL REFERENCES schools.schools(id) ON DELETE CASCADE,
                                      label VARCHAR(20) NOT NULL, -- ex: 2024-2025
                                      start_date DATE NOT NULL,
                                      end_date DATE NOT NULL,
                                      active BOOLEAN DEFAULT false,
                                      CONSTRAINT uq_school_year UNIQUE (school_id, label)
);

CREATE TABLE schools.class_level_groups (
                                            id BIGSERIAL PRIMARY KEY,
                                            code VARCHAR(20) NOT NULL UNIQUE, -- ex: MAT, PRI, COL, LYC
                                            name VARCHAR(100) NOT NULL        -- ex: Maternelle, Primaire, Collège, Lycée
);

-- Niveaux génériques de référence (communs à toutes les écoles)
CREATE TABLE schools.class_levels (
                                      id BIGSERIAL PRIMARY KEY,
                                      code VARCHAR(20) NOT NULL UNIQUE, -- ex: CP, CE1, TLE
                                      name VARCHAR(100) NOT NULL,        -- libellé complet
                                      group_id BIGINT REFERENCES schools.class_level_groups(id) ON DELETE SET NULL
);

-- Classes réelles pour une année scolaire donnée
CREATE TABLE schools.school_classes (
                                        id BIGSERIAL PRIMARY KEY,
                                        year_id BIGINT NOT NULL REFERENCES schools.school_years(id) ON DELETE CASCADE,
                                        level_id BIGINT NOT NULL REFERENCES schools.class_levels(id) ON DELETE CASCADE,
                                        name VARCHAR(50) NOT NULL, -- ex: CP1, CE1-B
                                        CONSTRAINT uq_school_class UNIQUE (year_id, level_id, name)
);

-- Élèves (infos stables, pas d'école directe ici)
CREATE TABLE schools.students (
                                  id BIGSERIAL PRIMARY KEY,
                                  first_name VARCHAR(100) NOT NULL,
                                  last_name VARCHAR(100) NOT NULL,
                                  birth_date DATE,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Inscription d'un élève dans une école/année/classe
CREATE TABLE schools.enrollments (
                                     id BIGSERIAL PRIMARY KEY,
                                     student_id BIGINT NOT NULL REFERENCES schools.students(id) ON DELETE CASCADE,
                                     class_id BIGINT NOT NULL REFERENCES schools.school_classes(id) ON DELETE CASCADE,
                                     enrolled_on DATE DEFAULT CURRENT_DATE,
                                     left_on DATE NULL,
                                     note VARCHAR(255),
                                     CONSTRAINT uq_enrollment UNIQUE (student_id, class_id)
);

-- Frais scolaires (liés uniquement à une classe réelle)
CREATE TABLE schools.fees (
                              id BIGSERIAL PRIMARY KEY,
                              class_id BIGINT NOT NULL REFERENCES schools.school_classes(id) ON DELETE CASCADE,
                              name VARCHAR(100) NOT NULL,
                              amount NUMERIC(10,2) NOT NULL,
                              due_date DATE,
                              description TEXT
);

-- Paiements (toujours liés à une inscription + un frais)
CREATE TABLE schools.payments (
                                  id BIGSERIAL PRIMARY KEY,
                                  enrollment_id BIGINT NOT NULL REFERENCES schools.enrollments(id) ON DELETE CASCADE,
                                  fee_id BIGINT NOT NULL REFERENCES schools.fees(id) ON DELETE CASCADE,
                                  paid_amount NUMERIC(10,2) NOT NULL,
                                  payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  payment_method VARCHAR(50),
                                  status VARCHAR(20) DEFAULT 'PAID' -- ex: PAID, PARTIAL, PENDING
);

-- Indexes
CREATE INDEX idx_students_created ON schools.students(created_at);
CREATE INDEX idx_enrollments_student ON schools.enrollments(student_id);
CREATE INDEX idx_enrollments_class ON schools.enrollments(class_id);
CREATE INDEX idx_payments_enrollment ON schools.payments(enrollment_id);

INSERT INTO schools.class_level_groups (code, name) VALUES ('MAT', 'Maternelle');
INSERT INTO schools.class_level_groups (code, name) VALUES ('PRI', 'Primaire');
INSERT INTO schools.class_level_groups (code, name) VALUES ('COL', 'Collège');
INSERT INTO schools.class_level_groups (code, name) VALUES ('LYC', 'Lycée');

INSERT INTO schools.class_levels (code, name, group_id) VALUES ('PS', 'Petite Section', (SELECT id FROM schools.class_level_groups WHERE code='MAT'));
INSERT INTO schools.class_levels (code, name, group_id) VALUES ('MS', 'Moyenne Section', (SELECT id FROM schools.class_level_groups WHERE code='MAT'));
INSERT INTO schools.class_levels (code, name, group_id) VALUES ('GS', 'Grande Section', (SELECT id FROM schools.class_level_groups WHERE code='MAT'));

INSERT INTO schools.class_levels (code, name, group_id) VALUES ('CP', 'Cours Préparatoire', (SELECT id FROM schools.class_level_groups WHERE code='PRI'));
INSERT INTO schools.class_levels (code, name, group_id) VALUES ('CE1', 'Cours Élémentaire 1', (SELECT id FROM schools.class_level_groups WHERE code='PRI'));
INSERT INTO schools.class_levels (code, name, group_id) VALUES ('CE2', 'Cours Élémentaire 2', (SELECT id FROM schools.class_level_groups WHERE code='PRI'));
INSERT INTO schools.class_levels (code, name, group_id) VALUES ('CM1', 'Cours Moyen 1', (SELECT id FROM schools.class_level_groups WHERE code='PRI'));
INSERT INTO schools.class_levels (code, name, group_id) VALUES ('CM2', 'Cours Moyen 2', (SELECT id FROM schools.class_level_groups WHERE code='PRI'));

INSERT INTO schools.class_levels (code, name, group_id) VALUES ('6E', 'Sixième', (SELECT id FROM schools.class_level_groups WHERE code='COL'));
INSERT INTO schools.class_levels (code, name, group_id) VALUES ('5E', 'Cinquième', (SELECT id FROM schools.class_level_groups WHERE code='COL'));
INSERT INTO schools.class_levels (code, name, group_id) VALUES ('4E', 'Quatrième', (SELECT id FROM schools.class_level_groups WHERE code='COL'));
INSERT INTO schools.class_levels (code, name, group_id) VALUES ('3E', 'Troisième', (SELECT id FROM schools.class_level_groups WHERE code='COL'));

INSERT INTO schools.class_levels (code, name, group_id) VALUES ('2ND', 'Seconde', (SELECT id FROM schools.class_level_groups WHERE code='LYC'));
INSERT INTO schools.class_levels (code, name, group_id) VALUES ('1ERE', 'Première', (SELECT id FROM schools.class_level_groups WHERE code='LYC'));
INSERT INTO schools.class_levels (code, name, group_id) VALUES ('TLE', 'Terminale', (SELECT id FROM schools.class_level_groups WHERE code='LYC'));
