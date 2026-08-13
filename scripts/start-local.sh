#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
compose_file="$root_dir/infra/docker-compose/docker-compose.yml"
env_file="$root_dir/infra/docker-compose/.env"

cd "$root_dir"

if [ -f "$env_file" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$env_file"
  set +a
fi

if [ ! -d node_modules ]; then
  npm install
fi

docker compose -f "$compose_file" up -d

echo "Waiting for Core API and importing local family data..."
node scripts/seed-local-data.mjs

echo
echo "WishPool local runtime is ready."
echo "Parent Web: http://localhost:${PARENT_WEB_PORT:-3000}"
echo "Admin Web:  http://localhost:${ADMIN_WEB_PORT:-3001}"
echo "Admin API token: ${WISHPOOL_ADMIN_TOKEN:-wishpool-local-admin-token}"
