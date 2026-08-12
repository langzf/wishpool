#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MIGRATION_DIR="${ROOT_DIR}/db/migrations"

test -d "${MIGRATION_DIR}"
test -f "${MIGRATION_DIR}/V001__initial_schema.sql"

python3 - <<'PY'
from pathlib import Path
import re

migration_dir = Path("db/migrations")
files = sorted(migration_dir.glob("V*__*.sql"))
if not files:
    raise SystemExit("No Flyway migrations found.")

versions = []
for path in files:
    match = re.fullmatch(r"V(\d+)__[\w_]+\.sql", path.name)
    if not match:
        raise SystemExit(f"Invalid Flyway migration name: {path.name}")
    versions.append(int(match.group(1)))
    text = path.read_text()
    if "create table" not in text.lower() and "alter table" not in text.lower():
        raise SystemExit(f"Migration has no schema statements: {path.name}")

expected = list(range(1, len(versions) + 1))
if versions != expected:
    raise SystemExit(f"Flyway versions must be contiguous from 1: {versions}")

print(f"validated {len(files)} Flyway migration(s)")
PY
