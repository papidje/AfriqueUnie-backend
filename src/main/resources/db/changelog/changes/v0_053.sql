--liquibase formatted sql
--changeset friasoft:053-tenant-is-active
--comment: Activation / désactivation d’un tenant (défaut actif).

ALTER TABLE schools.tenants
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE schools.tenants SET is_active = TRUE WHERE is_active IS NULL;

CREATE INDEX IF NOT EXISTS idx_tenants_is_active ON schools.tenants (is_active);
