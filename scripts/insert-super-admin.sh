#!/usr/bin/env bash
# Insère en base un utilisateur SUPER_ADMIN (superadmin@aaa.com) s’il n’existe pas.
#
# Mot de passe initial : SuperAdmin123!
#
# Variables d’environnement (optionnelles, défauts alignés sur application.yml) :
#   DB_HOST      localhost
#   DB_PORT      5432
#   DB_USERNAME  postgres
#   DB_PASSWORD  postgres
#   DB_NAME      schoolapp2
#
# Usage :
#   chmod +x scripts/insert-super-admin.sh
#   ./scripts/insert-super-admin.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SQL_FILE="${ROOT_DIR}/scripts/insert-super-admin.sql"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USERNAME="${DB_USERNAME:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"
DB_NAME="${DB_NAME:-schoolapp2}"

export PGPASSWORD="${DB_PASSWORD}"

psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USERNAME}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 -f "${SQL_FILE}"

echo "Terminé. Connexion : superadmin@aaa.com / SuperAdmin123! (à modifier après première connexion)."
