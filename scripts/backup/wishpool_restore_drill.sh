#!/usr/bin/env bash
set -Eeuo pipefail

BACKUP_DIR="${1:-}"
KEEP_DRILL=0
if [[ "${2:-}" == "--keep-drill" ]]; then
  KEEP_DRILL=1
elif [[ "${2:-}" == "--cleanup-drill" ]]; then
  KEEP_DRILL=0
elif [[ -n "${2:-}" ]]; then
  echo "usage: $0 /mnt/d/wishpool-backups/YYYYMMDD-HHmmss [--keep-drill|--cleanup-drill]" >&2
  exit 2
fi
[[ -f "$BACKUP_DIR/wishpool.dump" && -d "$BACKUP_DIR/media" && -f "$BACKUP_DIR/manifest.json" ]] || { echo "usage: $0 /mnt/d/wishpool-backups/YYYYMMDD-HHmmss" >&2; exit 2; }
for cmd in docker python3 curl; do command -v "$cmd" >/dev/null || { echo "missing command: $cmd" >&2; exit 1; }; done

NAS_HOST="${WISHPOOL_NAS_HOST:-Z4Pro-69HO.local}"
PG_PORT="${WISHPOOL_PG_PORT:-5432}"
PG_USER="${WISHPOOL_DATASOURCE_USERNAME:-wishpool_app}"
MINIO_ENDPOINT="${WISHPOOL_S3_ENDPOINT:-http://${NAS_HOST}:9000}"
PUBLIC_ENDPOINT="${WISHPOOL_S3_PUBLIC_ENDPOINT:-https://minio.yueying.cloud}"
TS="$(date +%Y%m%d-%H%M%S)"
DRILL_DB="wishpool_drill_${TS}"
DRILL_BUCKET=wishpool-media-drill

core_env() { docker inspect wishpool-core-api --format '{{range .Config.Env}}{{println .}}{{end}}' | awk -F= -v k="$1" '$1==k {sub(/^[^=]*=/, ""); print; exit}'; }
PG_PASSWORD="${WISHPOOL_DATASOURCE_PASSWORD:-$(core_env WISHPOOL_DATASOURCE_PASSWORD)}"
MINIO_ACCESS="${WISHPOOL_DRILL_S3_ACCESS_KEY:-${WISHPOOL_S3_ACCESS_KEY:-$(core_env WISHPOOL_S3_ACCESS_KEY)}}"
MINIO_SECRET="${WISHPOOL_DRILL_S3_SECRET_KEY:-${WISHPOOL_S3_SECRET_KEY:-$(core_env WISHPOOL_S3_SECRET_KEY)}}"
[[ -n "$PG_PASSWORD" && -n "$MINIO_ACCESS" && -n "$MINIO_SECRET" ]] || { echo "credentials not found" >&2; exit 1; }
PG=(docker run --rm --pull=never -e "PGPASSWORD=$PG_PASSWORD" postgres:16-alpine)

MC_CONFIG_DIR_HOST="$(mktemp -d "${TMPDIR:-/tmp}/wishpool-mc-config.XXXXXX")"
cleanup_drill() {
  local rc=$?
  set +e
  if (( KEEP_DRILL == 0 )); then
    echo "[drill] cleanup: $DRILL_DB and $DRILL_BUCKET"
    docker run --rm --pull=never -e MC_CONFIG_DIR=/tmp/mc -v "$MC_CONFIG_DIR_HOST:/tmp/mc" \
      minio/mc rm --recursive --force nas/"$DRILL_BUCKET" >/dev/null 2>&1
    docker run --rm --pull=never -e MC_CONFIG_DIR=/tmp/mc -v "$MC_CONFIG_DIR_HOST:/tmp/mc" \
      minio/mc rb --force nas/"$DRILL_BUCKET" >/dev/null 2>&1
    "${PG[@]}" psql -h "$NAS_HOST" -p "$PG_PORT" -U "$PG_USER" -d postgres -v ON_ERROR_STOP=0 \
      -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DRILL_DB' AND pid <> pg_backend_pid();" >/dev/null 2>&1
    "${PG[@]}" psql -h "$NAS_HOST" -p "$PG_PORT" -U "$PG_USER" -d postgres -v ON_ERROR_STOP=0 \
      -c "DROP DATABASE IF EXISTS \"$DRILL_DB\";" >/dev/null 2>&1
  else
    echo "[drill] kept: $DRILL_DB, $DRILL_BUCKET"
  fi
  docker run --rm --pull=never --entrypoint /bin/sh -v "$MC_CONFIG_DIR_HOST:/tmp/mc" minio/mc \
    -c 'rm -rf /tmp/mc/* /tmp/mc/.[!.]* /tmp/mc/..?*' >/dev/null 2>&1 || true
  rmdir "$MC_CONFIG_DIR_HOST" 2>/dev/null || rm -rf "$MC_CONFIG_DIR_HOST" 2>/dev/null || true
  return "$rc"
}
trap cleanup_drill EXIT

echo "[drill] WILL CREATE DATABASE: $DRILL_DB"
echo "[drill] WILL CREATE BUCKET: $DRILL_BUCKET"
if (( KEEP_DRILL == 0 )); then
  echo "[drill] successful or failed run will clean these drill objects"
else
  echo "[drill] --keep-drill: objects will remain for inspection"
fi
[[ "$DRILL_DB" == wishpool_drill_* && "$DRILL_BUCKET" == wishpool-media-drill ]] || { echo "unsafe names" >&2; exit 1; }
if [[ -n "${WISHPOOL_DRILL_CONFIRM:-}" ]]; then
  confirm="$WISHPOOL_DRILL_CONFIRM"
else
  read -r -p "Type CREATE to continue: " confirm || { echo "aborted" >&2; exit 1; }
fi
[[ "$confirm" == CREATE ]] || { echo "aborted"; exit 1; }

echo "[drill] create only new database"
"${PG[@]}" psql -h "$NAS_HOST" -p "$PG_PORT" -U "$PG_USER" -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE \"$DRILL_DB\" OWNER \"$PG_USER\";"
echo "[drill] restore into $DRILL_DB"
docker run --rm --pull=never -e "PGPASSWORD=$PG_PASSWORD" -v "$BACKUP_DIR:/backup:ro" postgres:16-alpine \
  pg_restore --no-owner --no-acl -h "$NAS_HOST" -p "$PG_PORT" -U "$PG_USER" -d "$DRILL_DB" /backup/wishpool.dump

COUNT_SQL="SELECT x.table_name, x.n FROM (SELECT 'family' table_name, count(*) n FROM family UNION ALL SELECT 'child_profile',count(*) FROM child_profile UNION ALL SELECT 'task_instance',count(*) FROM task_instance UNION ALL SELECT 'submission',count(*) FROM submission UNION ALL SELECT 'review',count(*) FROM review UNION ALL SELECT 'media_asset',count(*) FROM media_asset UNION ALL SELECT 'wish',count(*) FROM wish UNION ALL SELECT 'outbox_event',count(*) FROM outbox_event) x ORDER BY x.table_name;"
PROD_COUNTS="$( "${PG[@]}" psql -h "$NAS_HOST" -p "$PG_PORT" -U "$PG_USER" -d wishpool -At -F $'\t' -c "$COUNT_SQL" )"
DRILL_COUNTS="$( "${PG[@]}" psql -h "$NAS_HOST" -p "$PG_PORT" -U "$PG_USER" -d "$DRILL_DB" -At -F $'\t' -c "$COUNT_SQL" )"
printf '%s\n' "$PROD_COUNTS" > "$BACKUP_DIR/drill_production_counts.tsv"
printf '%s\n' "$DRILL_COUNTS" > "$BACKUP_DIR/drill_restored_counts.tsv"
python3 - "$PROD_COUNTS" "$DRILL_COUNTS" <<'PY'
import sys
def parse(s): return dict(x.split("\t") for x in s.splitlines() if x.strip())
a,b=parse(sys.argv[1]),parse(sys.argv[2]); bad=False
print("table\tproduction\tdrill\tstatus")
for k in sorted(set(a)|set(b)):
    ok=a.get(k)==b.get(k); bad |= not ok
    print(f"{k}\t{a.get(k)}\t{b.get(k)}\t{'OK' if ok else 'MISMATCH'}")
if bad: raise SystemExit(1)
PY

echo "[drill] create only $DRILL_BUCKET and restore objects"
docker run --rm --pull=never -e MC_CONFIG_DIR=/tmp/mc -v "$MC_CONFIG_DIR_HOST:/tmp/mc" \
  minio/mc alias set nas "$MINIO_ENDPOINT" "$MINIO_ACCESS" "$MINIO_SECRET" --api S3v4 >/dev/null
docker run --rm --pull=never -e MC_CONFIG_DIR=/tmp/mc -v "$MC_CONFIG_DIR_HOST:/tmp/mc" \
  minio/mc mb --ignore-existing nas/"$DRILL_BUCKET"
docker run --rm --pull=never -e MC_CONFIG_DIR=/tmp/mc -v "$MC_CONFIG_DIR_HOST:/tmp/mc" -v "$BACKUP_DIR/media:/backup:ro" \
  minio/mc mirror --overwrite /backup nas/"$DRILL_BUCKET" >/dev/null
docker run --rm --pull=never -e MC_CONFIG_DIR=/tmp/mc -v "$MC_CONFIG_DIR_HOST:/tmp/mc" \
  minio/mc ls --recursive --json nas/"$DRILL_BUCKET" > "$BACKUP_DIR/drill_bucket_listing.jsonl"

python3 - "$BACKUP_DIR" "$PUBLIC_ENDPOINT" "$DRILL_BUCKET" "$MINIO_ENDPOINT" "$MINIO_ACCESS" "$MINIO_SECRET" <<'PY'
import datetime, hashlib, hmac, json, pathlib, random, subprocess, sys, urllib.parse
base=pathlib.Path(sys.argv[1]); public=sys.argv[2].rstrip("/"); bucket=sys.argv[3]
items=[]
for line in (base/"drill_bucket_listing.jsonl").read_text().splitlines():
    try: x=json.loads(line)
    except: continue
    key=x.get("key") or x.get("name")
    if key and x.get("size") is not None: items.append((key,int(x["size"])))
manifest=json.loads((base/"manifest.json").read_text()); expected=manifest["bucket"]
total=sum(x[1] for x in items)
print(f"object_count\t{len(items)}\nrestored_total_bytes\t{total}\nexpected_object_count\t{expected['object_count']}\nexpected_total_bytes\t{expected['total_bytes']}")
if len(items)!=expected["object_count"] or total!=expected["total_bytes"]: raise SystemExit("bucket totals mismatch")
for key,size in random.Random(0).sample(items, min(3,len(items))):
    u=urllib.parse.urlsplit(public); host=u.netloc; region="us-east-1"
    now=datetime.datetime.now(datetime.timezone.utc); amz=now.strftime("%Y%m%dT%H%M%SZ"); day=now.strftime("%Y%m%d")
    path="/"+bucket+"/"+urllib.parse.quote(key, safe="/")
    credential=sys.argv[5]+"/"+day+"/"+region+"/s3/aws4_request"
    q={"X-Amz-Algorithm":"AWS4-HMAC-SHA256","X-Amz-Credential":credential,"X-Amz-Date":amz,"X-Amz-Expires":"600","X-Amz-SignedHeaders":"host"}
    enc=lambda s: urllib.parse.quote(str(s), safe="-_.~")
    canonical_q="&".join(enc(k)+"="+enc(q[k]) for k in sorted(q))
    canonical="GET\n"+path+"\n"+canonical_q+"\nhost:"+host+"\n\nhost\nUNSIGNED-PAYLOAD"
    def hs(k,s): return hmac.new(k,s.encode(),hashlib.sha256).digest()
    kdate=hs(("AWS4"+sys.argv[6]).encode(),day); kreg=hs(kdate,region); ksvc=hs(kreg,"s3"); ksign=hs(ksvc,"aws4_request")
    scope=day+"/"+region+"/s3/aws4_request"; string="AWS4-HMAC-SHA256\n"+amz+"\n"+scope+"\n"+hashlib.sha256(canonical.encode()).hexdigest()
    q["X-Amz-Signature"]=hmac.new(ksign,string.encode(),hashlib.sha256).hexdigest()
    url=public.rstrip("/")+path+"?"+"&".join(enc(k)+"="+enc(q[k]) for k in sorted(q))
    got=subprocess.check_output(["curl","-fsS","-A","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36","-o","/dev/null","-w","%{http_code}\t%{size_download}",url],text=True).strip()
    status,n=got.split("\t"); print(f"presigned_get\t{key}\t{status}\t{n}\texpected={size}")
    if status!="200" or int(float(n))!=size: raise SystemExit("presigned GET mismatch")
PY
echo "[drill] PASS: $DRILL_DB, $DRILL_BUCKET"
