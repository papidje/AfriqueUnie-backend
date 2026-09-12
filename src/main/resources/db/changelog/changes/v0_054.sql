--liquibase formatted sql
--changeset friasoft:054-tenant-subscription-ends-on
--comment: Date de fin d’abonnement tenant (NULL = sans échéance).

ALTER TABLE schools.tenants
    ADD COLUMN IF NOT EXISTS subscription_ends_on DATE;

CREATE INDEX IF NOT EXISTS idx_tenants_subscription_ends_on
    ON schools.tenants (subscription_ends_on);
