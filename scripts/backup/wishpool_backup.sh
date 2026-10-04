#!/usr/bin/env bash
set -Eeuo pipefail

NAS_HOST="${WISHPOOL_NAS_HOST:-Z4Pro-69HO.local}"
PG_PORT="${WISHPOOL_PG_PORT:-5432}"
PG_DB=wishpool
PG_USER="${WISHPOOL_DATASOURCE_USERNAME:-wishpool_app}"
MINIO_ENDPOINT="${WISHPOOL_S3_ENDPOINT:-http://${NAS_HOST}:9000}"
MINIO_BUCKET="${WISHPOOL_S3_BUCKET:-wishpool-media}"
BACKUP_ROOT="${WISHPOOL_BACKUP_ROOT:-/mnt/d/wishpool-backups}"
TS="$(date +%Y%m%d-%H%M%S)"
OUT_DIR="${BACKUP_ROOT}/${TS}"
MEDIA_DIR="${OUT_DIR}/media"
DB_DUMP="${OUT_DIR}/wishpool.dump"

die() { echo "ERROR: $*" >&2; exit 1; }
for cmd in docker python3 sha256sum; do command -v "$cmd" >/dev/null || die "missing command: $cmd"; done
[[ ! -e "$OUT_DIR" ]] || die "backup timestamp directory already exists: $OUT_DIR"
mkdir -p "$MEDIA_DIR"

core_env() {
  docker inspect wishpool-core-api --format '{{range .Config.Env}}{{println .}}{{end}}' |
    awk -F= -v k="$1" '$1 == k {sub(/^[^=]*=/, ""); print; exit}'
}
PG_PASSWORD="${WISHPOOL_DATASOURCE_PASSWORD:-$(core_env WISHPOOL_DATASOURCE_PASSWORD)}"
MINIO_ACCESS="${WISHPOOL_S3_ACCESS_KEY:-$(core_env WISHPOOL_S3_ACCESS_KEY)}"
MINIO_SECRET="${WISHPOOL_S3_SECRET_KEY:-$(core_env WISHPOOL_S3_SECRET_KEY)}"
[[ -n "$PG_PASSWORD" && -n "$MINIO_ACCESS" && -n "$MINIO_SECRET" ]] || die "credentials not found"

PG=(docker run --rm --pull=never -e "PGPASSWORD=$PG_PASSWORD" postgres:16-alpine)
echo "[backup] output: $OUT_DIR"
echo "[backup] production DB: $NAS_HOST:$PG_PORT/$PG_DB (read-only)"
echo "[backup] production bucket: $MINIO_ENDPOINT/$MINIO_BUCKET (read-only)"

echo "[backup] production key-table snapshot"
COUNTS="$( "${PG[@]}" psql -h "$NAS_HOST" -p "$PG_PORT" -U "$PG_USER" -d "$PG_DB" -At -F $'\t' -c "
SELECT table_name, n::bigint FROM (
 SELECT 'family' table_name, count(*) n FROM family
 UNION ALL SELECT 'child_profile',count(*) FROM child_profile
 UNION ALL SELECT 'task_instance',count(*) FROM task_instance
 UNION ALL SELECT 'submission',count(*) FROM submission
 UNION ALL SELECT 'review',count(*) FROM review
 UNION ALL SELECT 'media_asset',count(*) FROM media_asset
 UNION ALL SELECT 'wish',count(*) FROM wish
 UNION ALL SELECT 'outbox_event',count(*) FROM outbox_event
) x ORDER BY table_name;" )"
printf '%s\n' "$COUNTS" > "$OUT_DIR/db_counts.tsv"

echo "[backup] PostgreSQL custom-format dump"
"${PG[@]}" pg_dump -Fc --no-owner --no-acl -h "$NAS_HOST" -p "$PG_PORT" -U "$PG_USER" -d "$PG_DB" > "$DB_DUMP"
[[ -s "$DB_DUMP" ]] || die "empty PostgreSQL dump"

echo "[backup] MinIO read-only mirror"
MC_CONFIG_DIR_HOST="$(mktemp -d "${TMPDIR:-/tmp}/wishpool-mc-config.XXXXXX")"
cleanup_mc_config() {
  docker run --rm --pull=never --entrypoint /bin/sh -v "$MC_CONFIG_DIR_HOST:/tmp/mc" minio/mc \
    -c 'rm -rf /tmp/mc/* /tmp/mc/.[!.]* /tmp/mc/..?*' >/dev/null 2>&1 || true
  rmdir "$MC_CONFIG_DIR_HOST" 2>/dev/null || rm -rf "$MC_CONFIG_DIR_HOST" 2>/dev/null || true
}
trap cleanup_mc_config EXIT
docker run --rm --pull=never -e MC_CONFIG_DIR=/tmp/mc -v "$MC_CONFIG_DIR_HOST:/tmp/mc" \
  minio/mc alias set nas "$MINIO_ENDPOINT" "$MINIO_ACCESS" "$MINIO_SECRET" --api S3v4 >/dev/null
docker run --rm --pull=never -e MC_CONFIG_DIR=/tmp/mc -v "$MC_CONFIG_DIR_HOST:/tmp/mc" -v "$MEDIA_DIR:/backup" \
  minio/mc mirror --overwrite nas/"$MINIO_BUCKET" /backup >/dev/null

echo "[backup] checksums and manifest"
python3 - "$OUT_DIR" "$COUNTS" "$NAS_HOST" "$PG_DB" "$MINIO_BUCKET" <<'PY'
import hashlib, json, pathlib, sys
from datetime import datetime, timezone
out = pathlib.Path(sys.argv[1])
counts = dict(line.split("\t", 1) for line in sys.argv[2].splitlines() if line.strip())
files = []
for p in sorted(out.rglob("*")):
    if p.is_file() and p.name != "manifest.json":
        h, n = hashlib.sha256(), 0
        with p.open("rb") as f:
            for b in iter(lambda: f.read(1024*1024), b""):
                h.update(b); n += len(b)
        files.append({"path": p.relative_to(out).as_posix(), "bytes": n, "sha256": h.hexdigest()})
media = [x for x in files if x["path"].startswith("media/")]
manifest = {
  "format": 1, "created_at_utc": datetime.now(timezone.utc).isoformat(),
  "timestamp": out.name,
  "database": {"host": sys.argv[3], "name": sys.argv[4], "row_counts": {k:int(v) for k,v in counts.items()}},
  "bucket": {"name": sys.argv[5], "object_count": len(media), "total_bytes": sum(x["bytes"] for x in media)},
  "files": files,
  "commands": {
    "pg_dump": f"docker run --rm --pull=never -e PGPASSWORD=*** postgres:16-alpine pg_dump -Fc --no-owner --no-acl -h {sys.argv[3]} -p 5432 -U wishpool_app -d wishpool",
    "mc_mirror": f"docker run --rm --pull=never -v {out}/media:/backup minio/mc alias set nas <endpoint> *** *** && mc mirror nas/{sys.argv[5]} /backup"
  },
  "notes": ["Production DB and bucket accessed read-only.", "Temporal databases not included."]
}
(out/"manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
print(json.dumps({"db_counts":manifest["database"]["row_counts"],"object_count":len(media),"total_bytes":manifest["bucket"]["total_bytes"],"files":len(files)}, ensure_ascii=False))
PY
echo "[backup] pg_restore catalog"
docker run --rm --pull=never -i -e "PGPASSWORD=$PG_PASSWORD" postgres:16-alpine pg_restore -l < "$DB_DUMP" > "$OUT_DIR/pg_restore_catalog.txt"
sha256sum "$DB_DUMP" "$OUT_DIR/manifest.json" > "$OUT_DIR/SHA256SUMS"
echo "[backup] complete: $OUT_DIR"
du -sh "$OUT_DIR" "$DB_DUMP" "$MEDIA_DIR"
cat "$OUT_DIR/db_counts.tsv"
cat "$OUT_DIR/SHA256SUMS"
