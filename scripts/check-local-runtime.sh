#!/usr/bin/env sh
set -eu

test -x scripts/start-local.sh
test -x scripts/seed-local-data.mjs

node --check scripts/seed-local-data.mjs
bash -n scripts/start-local.sh

rg -n "local:start|local:seed" package.json >/dev/null
rg -n "seed-local-data|start-local" README.md infra/docker-compose/README.md infra/MODULE.md >/dev/null

echo "validated local runtime scripts"
