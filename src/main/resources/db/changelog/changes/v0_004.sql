--liquibase formatted sql
--changeset friasoft:004-school-tenant-id
--comment: Lien école ↔ tenant (inscription admin d’école).

ALTER TABLE schools.schools ADD COLUMN IF NOT EXISTS tenant_id BIGINT REFERENCES schools.tenants(id);

CREATE INDEX IF NOT EXISTS idx_schools_tenant_id ON schools.schools(tenant_id);
