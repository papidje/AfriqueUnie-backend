--liquibase formatted sql
--changeset friasoft:001-baseline-core
--comment: Schéma initial — tenants, écoles, utilisateurs avec les 3 rôles (pas de tables roles/users_roles).

CREATE SCHEMA IF NOT EXISTS schools;

CREATE TABLE schools.tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    logo VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE schools.schools (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    adress VARCHAR(255),
    contact VARCHAR(255),
    open_date DATE,
    logo VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE schools.users (
    id BIGSERIAL PRIMARY KEY,
    fullname VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    email VARCHAR(255),
    password VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    school_id BIGINT REFERENCES schools.schools(id),
    tenant_id BIGINT REFERENCES schools.tenants(id),
    last_login_at TIMESTAMP,
    role VARCHAR(30) NOT NULL DEFAULT 'STAFF',
    CONSTRAINT chk_users_role CHECK (role IN ('SUPER_ADMIN', 'ADMIN_ECOLE', 'STAFF'))
);

CREATE INDEX idx_users_tenant_id ON schools.users(tenant_id);
CREATE INDEX idx_users_school_id ON schools.users(school_id);
CREATE INDEX idx_users_email ON schools.users(email);

CREATE TABLE schools.activations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES schools.users(id),
    registration_date TIMESTAMP,
    code VARCHAR(6) NOT NULL,
    expiration TIMESTAMP
);

CREATE TABLE schools.refresh_token (
    id BIGSERIAL PRIMARY KEY,
    expired BOOLEAN NOT NULL,
    value VARCHAR(255),
    creation TIMESTAMP NOT NULL,
    expiration TIMESTAMP NOT NULL
);

CREATE TABLE schools.jwts (
    id BIGSERIAL PRIMARY KEY,
    jwt TEXT,
    is_active BOOLEAN NOT NULL,
    is_expired BOOLEAN NOT NULL,
    created_at TIMESTAMP,
    last_login_at TIMESTAMP,
    user_id BIGINT REFERENCES schools.users(id),
    refresh_token_id BIGINT REFERENCES schools.refresh_token(id)
);
