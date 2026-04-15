--liquibase formatted sql
--changeset friasoft:002-school-domain
--comment: Années scolaires, classes, élèves, frais, paiements, tables d’audit et données de référence niveaux.

CREATE TABLE schools.school_years (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools.schools(id) ON DELETE CASCADE,
    label VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT REFERENCES schools.users(id),
    updated_by BIGINT REFERENCES schools.users(id),
    CONSTRAINT uq_school_year UNIQUE (school_id, label)
);

CREATE TABLE schools.class_level_groups (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE schools.class_levels (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    group_id BIGINT REFERENCES schools.class_level_groups(id) ON DELETE SET NULL
);

CREATE TABLE schools.school_classes (
    id BIGSERIAL PRIMARY KEY,
    year_id BIGINT NOT NULL REFERENCES schools.school_years(id) ON DELETE CASCADE,
    level_id BIGINT NOT NULL REFERENCES schools.class_levels(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT REFERENCES schools.users(id),
    updated_by BIGINT REFERENCES schools.users(id),
    CONSTRAINT uq_school_class UNIQUE (year_id, level_id, name)
);

CREATE TABLE schools.students (
    id BIGSERIAL PRIMARY KEY,
    civility VARCHAR(20) CHECK (civility IN ('MONSIEUR','MADAME')),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE,
    matricule VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT REFERENCES schools.users(id),
    updated_by BIGINT REFERENCES schools.users(id)
);

CREATE TABLE schools.enrollments (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES schools.students(id) ON DELETE CASCADE,
    class_id BIGINT NOT NULL REFERENCES schools.school_classes(id) ON DELETE CASCADE,
    enrolled_on DATE DEFAULT CURRENT_DATE,
    left_on DATE NULL,
    note VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT REFERENCES schools.users(id),
    updated_by BIGINT REFERENCES schools.users(id),
    CONSTRAINT uq_enrollment UNIQUE (student_id, class_id)
);

CREATE TABLE schools.fees (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES schools.school_classes(id) ON DELETE CASCADE,
    name VARCHAR(100) DEFAULT 'INSCRIPTION' CHECK (name IN ('INSCRIPTION','SCOLARITE')),
    amount NUMERIC(10,2) NOT NULL,
    due_date DATE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT REFERENCES schools.users(id),
    updated_by BIGINT REFERENCES schools.users(id)
);

CREATE TABLE schools.payments (
    id BIGSERIAL PRIMARY KEY,
    enrollment_id BIGINT NOT NULL REFERENCES schools.enrollments(id) ON DELETE CASCADE,
    fee_id BIGINT NOT NULL REFERENCES schools.fees(id) ON DELETE CASCADE,
    paid_amount NUMERIC(10,2) NOT NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PAID' CHECK (status IN ('PAID','PARTIAL','PENDING')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT REFERENCES schools.users(id),
    updated_by BIGINT REFERENCES schools.users(id),
    CONSTRAINT uq_payment UNIQUE (enrollment_id, fee_id)
);

CREATE TABLE schools.enrollments_audit (
    id BIGSERIAL PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATE','UPDATE','DELETE')),
    old_data JSONB,
    new_data JSONB,
    user_id BIGINT REFERENCES schools.users(id),
    action_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE schools.fees_audit (
    id BIGSERIAL PRIMARY KEY,
    fee_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATE','UPDATE','DELETE')),
    old_data JSONB,
    new_data JSONB,
    user_id BIGINT REFERENCES schools.users(id),
    action_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE schools.payments_audit (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATE','UPDATE','DELETE')),
    old_data JSONB,
    new_data JSONB,
    user_id BIGINT REFERENCES schools.users(id),
    action_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_students_created ON schools.students(created_at);
CREATE INDEX idx_enrollments_student ON schools.enrollments(student_id);
CREATE INDEX idx_enrollments_class ON schools.enrollments(class_id);
CREATE INDEX idx_fees_class ON schools.fees(class_id);
CREATE INDEX idx_school_classes_year ON schools.school_classes(year_id);
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
