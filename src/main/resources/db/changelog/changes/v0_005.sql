--liquibase formatted sql
--changeset friasoft:005-school-year-class-tenant-id
--comment: tenant_id sur années et classes pour le filtre Hibernate (JWT tenant_id).

ALTER TABLE schools.school_years
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT REFERENCES schools.tenants(id);

UPDATE schools.school_years sy
SET tenant_id = sch.tenant_id
FROM schools.schools sch
WHERE sy.school_id = sch.id
  AND sy.tenant_id IS NULL
  AND sch.tenant_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_school_years_tenant_id ON schools.school_years(tenant_id);

ALTER TABLE schools.school_classes
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT REFERENCES schools.tenants(id);

UPDATE schools.school_classes sc
SET tenant_id = sy.tenant_id
FROM schools.school_years sy
WHERE sc.year_id = sy.id
  AND sc.tenant_id IS NULL
  AND sy.tenant_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_school_classes_tenant_id ON schools.school_classes(tenant_id);
