--liquibase formatted sql
--changeset friasoft:010-parents-students-emergency
--comment: Préparation du lien élèves ↔ parents + contacts d'urgence.

CREATE TABLE IF NOT EXISTS schools.parents (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES schools.tenants(id),
    last_name VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    phone VARCHAR(40) NOT NULL,
    email VARCHAR(180),
    profession VARCHAR(150),
    address VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_parents_tenant_id ON schools.parents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_parents_phone ON schools.parents(phone);
CREATE UNIQUE INDEX IF NOT EXISTS uq_parents_tenant_phone ON schools.parents(tenant_id, phone);

ALTER TABLE schools.students
    ADD COLUMN IF NOT EXISTS father_id BIGINT REFERENCES schools.parents(id),
    ADD COLUMN IF NOT EXISTS mother_id BIGINT REFERENCES schools.parents(id),
    ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(40);

CREATE INDEX IF NOT EXISTS idx_students_father_id ON schools.students(father_id);
CREATE INDEX IF NOT EXISTS idx_students_mother_id ON schools.students(mother_id);
