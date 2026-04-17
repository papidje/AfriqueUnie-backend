--liquibase formatted sql
--changeset friasoft:011-student-tenant-schoolclass
--comment: tenant_id et lien Student ↔ SchoolClass.

ALTER TABLE schools.students
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT REFERENCES schools.tenants(id);

ALTER TABLE schools.students
    ADD COLUMN IF NOT EXISTS school_class_id BIGINT REFERENCES schools.school_classes(id);

CREATE INDEX IF NOT EXISTS idx_students_tenant_id ON schools.students(tenant_id);
CREATE INDEX IF NOT EXISTS idx_students_school_class_id ON schools.students(school_class_id);
