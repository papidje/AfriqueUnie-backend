-- Insère un SUPER_ADMIN si l’email superadmin@aaa.com n’existe pas encore.
--
-- Mot de passe initial (hash bcrypt ci‑dessous) : SuperAdmin123!
-- À changer après la première connexion.
--
-- Exemple :
--   export PGPASSWORD=postgres
--   psql -h localhost -p 5432 -U postgres -d schoolapp2 -v ON_ERROR_STOP=1 \
--     -f scripts/insert-super-admin.sql

INSERT INTO schools.users (
    fullname,
    username,
    email,
    password,
    is_active,
    role,
    tenant_id,
    school_id,
    created_at,
    updated_at,
    last_login_at
)
SELECT
    'Super administrateur',
    'superadmin@yopmail.com',
    'superadmin@yopmail.com',
    '$2y$10$Q7iITvmlPFu1aut6jRhT7OqkACYc7pSgqXkGMisuop5yitpXR7oAW',
    TRUE,
    'SUPER_ADMIN',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM schools.users WHERE email = 'superadmin@aaa.com'
);
