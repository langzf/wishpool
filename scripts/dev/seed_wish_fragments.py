#!/usr/bin/env python3
"""仅本地调试/验收用，勿在生产执行。

补齐指定 wish 的碎片并同步灯亮状态，用于验收解锁动画。
"""
import argparse
import json
import os
import re
import subprocess
import sys


def psql(database_url: str, sql: str) -> str:
    command = ["docker.exe", "run", "--rm", "postgres:16-alpine", "psql", database_url, "-At", "-c", sql]
    return subprocess.check_output(command, text=True).strip()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("wish_id", help="wish UUID")
    parser.add_argument("--database-url", default=os.environ.get("WISHPOOL_DATABASE_URL"))
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9a-fA-F-]{36}", args.wish_id):
        raise SystemExit("wish_id must be a UUID")
    if not args.database_url:
        raise SystemExit("set --database-url or WISHPOOL_DATABASE_URL")
    row = psql(args.database_url, f"select required_fragments, coalesce(fragment_mask_json::text, '{{}}') from wish where id = '{args.wish_id}';")
    if not row:
        raise SystemExit("wish not found")
    required_text, mask_text = row.split("\t", 1)
    required = int(required_text)
    mask = json.loads(mask_text)
    cells = mask.get("cells") or mask.get("fragments") or []
    total = len(cells) or required
    lit = list(range(min(required, total)))
    lit_json = json.dumps({"version": 1, "litIndexes": lit}, separators=(",", ":"))
    escaped = lit_json.replace("'", "''")
    sql = (
        "update wish set earned_fragments = required_fragments, "
        f"fragment_lit_json = '{escaped}'::jsonb where id = '{args.wish_id}';"
    )
    psql(args.database_url, sql)
    print(json.dumps({"wishId": args.wish_id, "earnedFragments": required, "requiredFragments": required, "litIndexes": lit}))
    return 0


if __name__ == "__main__":
    sys.exit(main())
