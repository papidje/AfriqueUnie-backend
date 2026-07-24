-- Insère un SUPER_ADMIN s’il n’existe pas encore (compatible post-v0_039).
--
-- Identifiants :
--   Email / username : superadmin@yopmail.com
--   Mot de passe     : SuperAdmin123!
-- À changer après la première connexion.
--
-- Schéma : le rôle plateforme vit dans schools.user_platform_roles
-- (la colonne schools.users.role a été supprimée en v0_039).
--
-- Exemple :
--   export PGPASSWORD=postgres
--   psql -h localhost -p 5432 -U postgres -d schoolapp2 -v ON_ERROR_STOP=1 \
--     -f scripts/insert-super-admin.sql
--
-- Via Docker :
--   docker compose exec -T db psql -U postgres -d schoolapp2 -v ON_ERROR_STOP=1 \
--     < AfriqueUnie-backend/scripts/insert-super-admin.sql

INSERT INTO schools.users (
    fullname,
    username,
    email,
    password,
    is_active,
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
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM schools.users
    WHERE lower(trim(email)) = lower(trim('superadmin@yopmail.com'))
);

-- Garantit le rôle plateforme même si l’utilisateur existait déjà sans ligne associée.
INSERT INTO schools.user_platform_roles (user_id, role)
SELECT u.id, 'SUPER_ADMIN'
FROM schools.users u
WHERE lower(trim(u.email)) = lower(trim('superadmin@yopmail.com'))
ON CONFLICT (user_id) DO UPDATE SET role = EXCLUDED.role;
