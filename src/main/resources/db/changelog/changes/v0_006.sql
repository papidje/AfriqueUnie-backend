--liquibase formatted sql
--changeset friasoft:006-subjects-class-subjects
--comment: Matières (référentiel) et liaison classe ↔ matière avec coefficient.

CREATE TABLE schools.subjects (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE schools.class_subjects (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES schools.school_classes(id) ON DELETE CASCADE,
    subject_id BIGINT NOT NULL REFERENCES schools.subjects(id) ON DELETE RESTRICT,
    coefficient INTEGER NOT NULL DEFAULT 1,
    tenant_id BIGINT REFERENCES schools.tenants(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_class_subject UNIQUE (class_id, subject_id)
);

CREATE INDEX idx_class_subjects_class_id ON schools.class_subjects(class_id);
CREATE INDEX idx_class_subjects_tenant_id ON schools.class_subjects(tenant_id);

INSERT INTO schools.subjects (code, name) VALUES ('MATH', 'Mathématiques');
INSERT INTO schools.subjects (code, name) VALUES ('FR', 'Français');
INSERT INTO schools.subjects (code, name) VALUES ('ANG', 'Anglais');
INSERT INTO schools.subjects (code, name) VALUES ('HIST', 'Histoire-Géographie');
INSERT INTO schools.subjects (code, name) VALUES ('SVT', 'Sciences de la vie et de la Terre');
INSERT INTO schools.subjects (code, name) VALUES ('PC', 'Physique-Chimie');
