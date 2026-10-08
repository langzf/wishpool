#!/usr/bin/env python3
"""WishPool end-to-end acceptance test (Python 3.11 standard library only)."""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import struct
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid
import zlib
from pathlib import Path
from typing import Any, Callable

ROOT = Path(__file__).resolve().parents[2]
RESULT_PATH = ROOT / "scripts" / "e2e" / "last_result.json"
CORE_API_URL = "http://localhost:18080"
PARENT_WEB_URL = "http://localhost:18300"
REALTIME_HEALTH_URL = "http://localhost:18081/health"
PHONE_NUMBER = "18729042096"
FAMILY_ID = "6f6a9246-5ab7-4abe-aee8-639559b69243"
CHILD_ID = "47fecf11-ec9d-405b-8049-281fc61fac0f"
INTERNAL_TOKEN = "wishpool-local-internal-token"
BROWSER_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"


class HttpResult:
    def __init__(self, status: int, body: Any, text: str, headers: dict[str, str]):
        self.status, self.body, self.text, self.headers = status, body, text, headers


class HttpClient:
    def __init__(self, base_url: str, timeout: float = 12.0):
        self.base_url, self.timeout = base_url.rstrip("/"), timeout

    def request(self, method: str, path: str, token: str | None = None, body: Any = None,
                headers: dict[str, str] | None = None) -> HttpResult:
        url = path if path.startswith(("http://", "https://")) else self.base_url + path
        request_headers = {"Accept": "application/json"}
        if body is not None:
            request_headers["Content-Type"] = "application/json"
        if token:
            request_headers["Authorization"] = f"Bearer {token}"
        if headers:
            request_headers.update(headers)
        data = json.dumps(body, ensure_ascii=False).encode() if body is not None else None
        request = urllib.request.Request(url, data=data, headers=request_headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=self.timeout) as response:
                text = response.read().decode("utf-8", errors="replace")
                return HttpResult(response.status, decode_body(text), text, dict(response.headers.items()))
        except urllib.error.HTTPError as exc:
            text = exc.read().decode("utf-8", errors="replace")
            return HttpResult(exc.code, decode_body(text), text, dict(exc.headers.items()))
        except (urllib.error.URLError, TimeoutError, OSError) as exc:
            raise RuntimeError(f"HTTP {method} {url}: {exc}") from exc


def decode_body(text: str) -> Any:
    if not text:
        return None
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return text


def compact(value: Any, limit: int = 420) -> str:
    text = value if isinstance(value, str) else json.dumps(value, ensure_ascii=False, separators=(",", ":"))
    return text if len(text) <= limit else text[:limit] + "..."


def png_32() -> bytes:
    width = height = 32
    raw = (b"\x00" + b"\x32\x99\xff" * width) * height

    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)

    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))


class Runner:
    def __init__(self, args: argparse.Namespace):
        self.args = args
        self.core = HttpClient(args.core_api_url, args.http_timeout)
        self.parent_token: str | None = None
        self.child_token: str | None = None
        self.plan: dict[str, Any] | None = None
        self.task: dict[str, Any] | None = None
        self.submission: dict[str, Any] | None = None
        self.results: list[dict[str, Any]] = []
        self.created_rows: list[dict[str, str]] = []
        self.run_id = args.run_id or dt.datetime.now().strftime("%H%M%S") + "-" + uuid.uuid4().hex[:6]
        self.prefix = f"e2e-{self.run_id}"
        self.week_id = f"e2e-w-{self.run_id}"
        self.task_title = self.prefix
        self.client_mutation_id = f"{self.prefix}-submission"
        self.plan_key = f"{self.prefix}-plan"
        self.reward_before: tuple[int, int] | None = None
        self.submission_initial_status: str | None = None

    def add(self, check_id: str, name: str, fn: Callable[[], tuple[bool, str]]) -> None:
        try:
            ok, evidence = fn()
            original = evidence
            if self.args.inject == check_id:
                ok, evidence = False, f"INJECTED expectation mismatch; original={original}"
        except Exception as exc:
            ok, evidence = False, f"{type(exc).__name__}: {exc}"
        result = {"id": check_id, "name": name, "result": "PASS" if ok else "FAIL", "evidence": evidence}
        self.results.append(result)
        print(f"[{result['result']}] {check_id} {name} - {evidence}")

    def db(self, sql: str) -> list[list[str]]:
        inspect = subprocess.run(["docker.exe", "inspect", "wishpool-core-api", "--format", "{{range .Config.Env}}{{println .}}{{end}}"], capture_output=True, text=True, timeout=20)
        if inspect.returncode != 0:
            raise RuntimeError(f"docker inspect failed: {inspect.stderr.strip()}")
        password = next((line.split("=", 1)[1] for line in inspect.stdout.splitlines() if line.startswith("WISHPOOL_DATASOURCE_PASSWORD=")), None)
        if not password:
            raise RuntimeError("WISHPOOL_DATASOURCE_PASSWORD not found")
        proc = subprocess.run(["docker.exe", "run", "--rm", "-e", f"PGPASSWORD={password}", "postgres:16-alpine", "psql", "-h", "Z4Pro-69HO.local", "-U", "wishpool_app", "-d", "wishpool", "-t", "-A", "-F", "|", "-c", sql], capture_output=True, text=True, timeout=35)
        if proc.returncode:
            raise RuntimeError(f"psql failed: {proc.stderr.strip()}")
        return [line.split("|") for line in proc.stdout.splitlines() if line.strip()]

    def db_one(self, sql: str) -> list[str] | None:
        rows = self.db(sql)
        return rows[0] if rows else None

    def login(self) -> tuple[bool, str]:
        code = self.core.request("POST", "/auth/phone-codes", body={"phoneNumber": PHONE_NUMBER, "purpose": "login"})
        if code.status != 200 or not isinstance(code.body, dict) or not code.body.get("verificationToken"):
            return False, f"phone-codes status={code.status} body={compact(code.body)}"
        login = self.core.request("POST", "/auth/login", body={"provider": "phone", "credential": f"{code.body['verificationToken']}:123456"})
        if login.status != 200 or not isinstance(login.body, dict) or not login.body.get("accessToken"):
            return False, f"login status={login.status} body={compact(login.body)}"
        self.parent_token = login.body["accessToken"]
        self.reward_before = self.reward_ledger_totals()
        return True, "real parent accessToken received"

    def save_plan(self) -> tuple[bool, str]:
        body = {"familyId": FAMILY_ID, "childId": CHILD_ID, "weekId": self.week_id, "startDate": self.args.date,
                "endDate": (dt.date.fromisoformat(self.args.date) + dt.timedelta(days=6)).isoformat(), "rewardMode": "flexible",
                "rules": [{"title": self.task_title, "category": "study", "submissionType": "photo", "weekdays": [1, 2, 3, 4, 5, 6, 7], "isCore": False, "requireReview": True, "sortOrder": 1}]}
        r = self.core.request("POST", "/plans", self.parent_token, body, {"Idempotency-Key": self.plan_key})
        if r.status != 200 or not isinstance(r.body, dict):
            return False, f"status={r.status} body={compact(r.body)}"
        self.plan = r.body
        self.created_rows += [{"table": "plan", "id": str(r.body.get("id"))}, {"table": "weekId", "id": self.week_id}, {"table": "rule", "id": self.task_title}]
        return True, f"planId={r.body.get('id')} weekId={self.week_id} rule={self.task_title} submissionType=photo isCore=false"

    def materialize(self) -> tuple[bool, str]:
        deadline = time.time() + self.args.poll_seconds
        last: Any = None
        while time.time() < deadline:
            r = self.core.request("GET", f"/children/{CHILD_ID}/today?date={self.args.date}", self.parent_token)
            last = r.body
            if r.status == 200 and isinstance(r.body, dict):
                matches = [t for t in r.body.get("tasks", []) if t.get("title") == self.task_title and t.get("scheduledDate") == self.args.date]
                if matches:
                    self.task = matches[0]
                    self.created_rows.append({"table": "task_instance", "id": str(self.task.get("id"))})
                    return True, f"taskId={self.task.get('id')} status={self.task.get('status')} date={self.args.date} poll<= {self.args.poll_seconds}s"
            time.sleep(2)
        return False, f"no {self.task_title}/todo within {self.args.poll_seconds}s; last={compact(last)}"

    def pair(self) -> tuple[bool, str]:
        r = self.core.request("POST", f"/families/{FAMILY_ID}/pairing-sessions", self.parent_token, {"childId": CHILD_ID}, {"Idempotency-Key": f"{self.prefix}-pair-{uuid.uuid4()}"})
        if r.status not in (200, 201) or not isinstance(r.body, dict) or not r.body.get("pairingCode"):
            return False, f"create pairing status={r.status} body={compact(r.body)}"
        consume = self.core.request("POST", "/pairing/consume", body={"pairingCode": r.body["pairingCode"], "device": {"platform": "android", "deviceName": self.prefix}}, headers={"Idempotency-Key": f"{self.prefix}-device-{uuid.uuid4()}"})
        if consume.status != 200 or not isinstance(consume.body, dict) or not consume.body.get("accessToken"):
            return False, f"consume status={consume.status} body={compact(consume.body)}"
        self.child_token = consume.body["accessToken"]
        device_id = consume.body.get("deviceId")
        if not device_id:
            row = self.db_one(f"select id from device where family_id='{FAMILY_ID}' and device_name='{self.prefix}' order by created_at desc limit 1")
            device_id = row[0] if row else None
        if device_id:
            self.created_rows.append({"table": "device", "id": str(device_id)})
        return True, "pairing consume returned real child token"

    def upload(self, related_id: str) -> str:
        data = png_32()
        r = self.core.request("POST", "/media/upload-sessions", self.child_token, {"familyId": FAMILY_ID, "childId": CHILD_ID, "purpose": "submission", "contentType": "image/png", "sizeBytes": len(data), "relatedResource": {"type": "task_instance", "id": related_id}}, {"Idempotency-Key": f"{self.prefix}-media-{related_id}"})
        if r.status != 201:
            raise RuntimeError(f"upload session status={r.status} body={compact(r.body)}")
        media_id, upload_url = r.body["mediaId"], r.body["uploadUrl"]
        # 直传对象存储：经 Cloudflare 隧道（minio.yueying.cloud）时有瞬时 502/503/504 抖动，
        # 首次即失败会把「基础设施抖动」误报成回归失败（2026-10-08 09:00 实测）。
        # presigned URL 在有效期内可重复 PUT，故对 5xx 与网络错误做 4 次退避重试。
        put_status, put_err = None, None
        for attempt in range(4):
            request = urllib.request.Request(upload_url, data=data, headers={"Content-Type": "image/png", "User-Agent": self.args.user_agent}, method="PUT")
            try:
                with urllib.request.urlopen(request, timeout=20) as response:
                    put_status = response.status
                if put_status in (200, 201):
                    put_err = None
                    break
                put_err = f"presigned PUT status={put_status}"
            except urllib.error.HTTPError as exc:
                put_err = f"presigned PUT status={exc.code} body={exc.read().decode(errors='replace')}"
                if exc.code not in (502, 503, 504) or attempt == 3:
                    raise RuntimeError(put_err) from exc
            except Exception as exc:
                put_err = f"presigned PUT error={exc}"
                if attempt == 3:
                    raise RuntimeError(put_err) from exc
            print(f"[RETRY] {put_err} — 隧道瞬时抖动，第 {attempt + 2}/4 次尝试", flush=True)
            time.sleep(2.0 * (attempt + 1))
        if put_err:
            raise RuntimeError(put_err)
        final = self.core.request("POST", f"/media/{media_id}/finalize", self.child_token, {"checksumSha256": hashlib.sha256(data).hexdigest(), "width": 32, "height": 32}, {"Idempotency-Key": f"{self.prefix}-finalize-{media_id}"})
        if final.status != 200:
            raise RuntimeError(f"finalize status={final.status} body={compact(final.body)}")
        self.created_rows.append({"table": "media_asset", "id": str(media_id)})
        return f"{media_id}|PUT={put_status}|finalize=200|media_asset.status=uploaded(at finalize)"

    def wait_media_status(self, media_id: str) -> str:
        deadline = time.time() + self.args.poll_seconds
        status = None
        while time.time() < deadline:
            row = self.db_one(f"select status from media_asset where id='{media_id}'")
            status = row[0] if row else None
            if status in ("ready", "processing"):
                return status
            time.sleep(2)
        raise RuntimeError(f"media_asset {media_id} final status={status!r}; expected ready or processing")

    def submit(self) -> tuple[bool, str]:
        if not self.task:
            return False, "materialized task unavailable"
        media_id = None
        media_evidence = "media disabled"
        if self.args.with_media:
            media_evidence = self.upload(self.task["id"])
            media_id = media_evidence.split("|", 1)[0]
        body = {"taskInstanceId": self.task["id"], "clientMutationId": self.client_mutation_id, "mediaAssetIds": [media_id] if media_id else []}
        r = self.core.request("POST", "/submissions", self.child_token, body, {"Idempotency-Key": f"{self.prefix}-submission"})
        if r.status != 201 or not isinstance(r.body, dict):
            return False, f"status={r.status} body={compact(r.body)}"
        self.submission = r.body
        self.submission_initial_status = r.body.get("status")
        self.created_rows.append({"table": "submission", "id": str(r.body.get("id"))})
        media_final = self.wait_media_status(media_id) if media_id else "n/a"
        return True, f"submissionId={r.body.get('id')} initialStatus={self.submission_initial_status} mediaAssetIds={body['mediaAssetIds']} {media_evidence}|media_asset.status={media_final}"

    def duplicate_submission(self) -> tuple[bool, str]:
        if not self.task or not self.submission:
            return False, "submission prerequisite unavailable"
        r = self.core.request("POST", "/submissions", self.child_token, {"taskInstanceId": self.task["id"], "clientMutationId": self.client_mutation_id, "mediaAssetIds": []}, {"Idempotency-Key": f"{self.prefix}-submission-duplicate"})
        same_id = isinstance(r.body, dict) and r.body.get("id") == self.submission.get("id")
        return (r.status == 409 or (r.status in (200, 201) and same_id), f"duplicate status={r.status} sameSubmission={same_id}")

    def ai(self) -> tuple[bool, str]:
        if not self.submission:
            return False, "submission prerequisite unavailable"
        sid = self.submission["id"]
        deadline, latest, status = time.time() + self.args.poll_seconds, None, self.submission_initial_status
        while time.time() < deadline:
            r = self.core.request("GET", f"/submissions/{sid}", self.child_token)
            latest = r.body
            if r.status == 200 and isinstance(r.body, dict):
                item = r.body.get("submission", r.body)
                status = item.get("status") if isinstance(item, dict) else None
                if status == "review_pending":
                    break
            time.sleep(2)
        row = self.db_one("select (select count(*) from ai_job where submission_id='%s'), (select count(*) from ai_precheck where submission_id='%s'), (select count(*) from model_invocation_log where ai_job_id in (select id from ai_job where submission_id='%s')), (select count(*) from outbox_event where event_type='submission.ai_prechecked' and aggregate_id='%s'), (select count(*) from ai_precheck ap join ai_job aj on aj.id=ap.ai_job_id where ap.submission_id='%s')" % (sid, sid, sid, sid, sid))
        counts = [int(v) for v in row] if row else [0] * 5
        ok = self.submission_initial_status == "ai_pending" and status == "review_pending" and counts == [1, 1, 1, 1, 1]
        return ok, f"status {self.submission_initial_status}->{status}; ai_job/ai_precheck/model_log/outbox/fk={counts}; body={compact(latest)}"

    def review_detail(self) -> tuple[bool, str]:
        if not self.submission:
            return False, "submission prerequisite unavailable"
        r = self.core.request("GET", f"/reviews/{self.submission['id']}/detail", self.parent_token)
        pre = r.body.get("aiPrecheck") if isinstance(r.body, dict) else None
        return r.status == 200 and pre is not None, f"status={r.status} aiPrecheck={'present' if pre is not None else 'null'}"

    def approve(self) -> tuple[bool, str]:
        if not self.submission:
            return False, "submission prerequisite unavailable"
        r = self.core.request("POST", "/reviews", self.parent_token, {"submissionId": self.submission["id"], "decision": "approved"}, {"Idempotency-Key": f"{self.prefix}-review"})
        return r.status == 201, f"status={r.status} reviewId={r.body.get('id') if isinstance(r.body, dict) else None}"

    def reward_ledger_totals(self) -> tuple[int, int]:
        row = self.db_one(f"select count(*), coalesce(sum(amount),0) from reward_ledger where child_id='{CHILD_ID}'")
        return (int(row[0]), int(row[1])) if row else (0, 0)

    def rewards_wish(self) -> tuple[bool, str]:
        deadline, before = time.time() + self.args.poll_seconds, self.reward_before or (0, 0)
        after = before
        while time.time() < deadline:
            after = self.reward_ledger_totals()
            if after[0] > before[0] or after[1] > before[1]:
                break
            time.sleep(2)
        summary = self.core.request("GET", f"/children/{CHILD_ID}/rewards/summary", self.parent_token)
        wish = self.core.request("GET", f"/children/{CHILD_ID}/wishes/current", self.parent_token)
        return after[0] > before[0] and after[1] > before[1] and summary.status == 200 and wish.status == 200, f"reward_ledger count/sum {before}->{after}; summary={summary.status} wish={wish.status}"

    def memories_room(self) -> tuple[bool, str]:
        memories = self.core.request("GET", f"/memories?childId={CHILD_ID}", self.parent_token)
        room = self.core.request("GET", f"/room/state?childId={CHILD_ID}", self.parent_token)
        items = room.body.get("items", []) if isinstance(room.body, dict) else []
        if memories.status != 200 or room.status != 200 or not items:
            return False, f"memories={memories.status} room={room.status} items={len(items)}"
        item = next((i for i in items if i.get("visible", True)), items[0])
        arrange = self.core.request("POST", f"/room/items/{item['id']}/arrange", self.parent_token, {"position": {"x": 4, "y": 2, "layer": 1}}, {"Idempotency-Key": f"{self.prefix}-arrange-{item['id']}"})
        return arrange.status == 200, f"memories={len(memories.body.get('items', []))} roomItems={len(items)} arrangeStatus={arrange.status}"

    def sync_realtime(self) -> tuple[bool, str]:
        first = self.core.request("GET", f"/sync/pull?familyId={FAMILY_ID}&afterSeq=0&limit=500", self.parent_token)
        if first.status != 200 or not isinstance(first.body, dict):
            return False, f"sync status={first.status} body={compact(first.body)}"
        latest = first.body.get("latestSeq", 0)
        second = self.core.request("GET", f"/sync/pull?familyId={FAMILY_ID}&afterSeq={latest}&limit=500", self.parent_token)
        health = HttpClient(REALTIME_HEALTH_URL).request("GET", "")
        return second.status == 200 and health.status == 200, f"latestSeq={latest}->{second.body.get('latestSeq') if isinstance(second.body, dict) else '?'} realtime={health.status}"

    def web_routes(self) -> tuple[bool, str]:
        routes = ["/", "/reviews", "/wish", "/plan", "/memories", "/room", "/notifications", "/settings/privacy", "/settings/family"]
        statuses = []
        for route in routes:
            try:
                with urllib.request.urlopen(urllib.request.Request(PARENT_WEB_URL + route, headers={"Accept": "text/html"}), timeout=12) as response:
                    statuses.append((route, response.status))
            except urllib.error.HTTPError as exc:
                statuses.append((route, exc.code))
            except Exception:
                statuses.append((route, 0))
        return all(status == 200 for _, status in statuses), " ".join(f"{route}={status}" for route, status in statuses)

    def outbox_zero(self) -> tuple[bool, str]:
        row = self.db_one("select count(*) from outbox_event where published_at is null")
        count = int(row[0]) if row else -1
        return count == 0, f"unpublished outbox={count}"

    def db_evidence(self) -> tuple[bool, str]:
        if not self.submission:
            return False, "submission unavailable"
        sid = self.submission["id"]
        rows = self.db("select 'submission|'||id||'|'||status from submission where id='%s' union all select 'ai_job|'||id||'|'||status from ai_job where submission_id='%s' union all select 'ai_precheck|'||id||'|'||ai_job_id from ai_precheck where submission_id='%s' union all select 'outbox|'||id||'|'||event_type from outbox_event where aggregate_id='%s' and event_type='submission.ai_prechecked'" % (sid, sid, sid, sid))
        evidence = "; ".join(row[0] for row in rows)
        return len(rows) >= 4, evidence or "no rows"

    def run(self) -> int:
        self.add("auth.login", "parent login", self.login)
        self.add("plan.materialize", "save unique week plan", lambda: self.save_plan() if self.parent_token else (False, "login prerequisite unavailable"))
        self.add("task.today", "poll today's materialized todo", lambda: self.materialize() if self.parent_token else (False, "login prerequisite unavailable"))
        self.add("pairing.consume", "pair child device", lambda: self.pair() if self.parent_token else (False, "login prerequisite unavailable"))
        self.add("submission.create", "create ai_pending submission", lambda: self.submit() if self.child_token else (False, "pairing prerequisite unavailable"))
        self.add("submission.idempotency", "duplicate submission is rejected/reused", self.duplicate_submission)
        self.add("ai.precheck", "AI four-evidence precheck", self.ai)
        self.add("review.detail", "review detail includes aiPrecheck", self.review_detail)
        self.add("review.approve", "approve review returns 201", self.approve)
        self.add("reward.ledger", "reward ledger grows", self.rewards_wish)
        self.add("memories.room", "memories and room", self.memories_room)
        self.add("sync.realtime", "sync and realtime health", self.sync_realtime)
        self.add("parent.routes", "parent web routes", self.web_routes)
        self.add("outbox.zero", "outbox drains", self.outbox_zero)
        self.add("db.evidence", "database evidence", self.db_evidence)
        payload = {"startedAt": dt.datetime.now(dt.timezone.utc).isoformat(), "date": self.args.date, "runId": self.run_id, "weekId": self.week_id, "baseUrls": {"coreApi": self.core.base_url, "parentWeb": PARENT_WEB_URL, "realtimeHealth": REALTIME_HEALTH_URL}, "results": self.results, "createdRows": self.created_rows, "prefix": self.prefix}
        RESULT_PATH.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        passed, total = sum(r["result"] == "PASS" for r in self.results), len(self.results)
        print(f"NEW ROWS (table + id): {json.dumps(self.created_rows, ensure_ascii=False)}")
        print(f"DATA PREFIX: plan={self.plan.get('id') if self.plan else None} weekId={self.week_id} rule/task title={self.task_title} task={self.task.get('id') if self.task else None} submission={self.submission.get('id') if self.submission else None}")
        print(f"SUMMARY {passed}/{total} PASS")
        print(f"RESULT {RESULT_PATH}")
        return 0 if passed == total else 1


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="WishPool real-runtime E2E acceptance")
    parser.add_argument("--core-api-url", default=CORE_API_URL)
    parser.add_argument("--inject", metavar="CHECK_ID")
    parser.add_argument("--date", default=dt.date.today().isoformat())
    parser.add_argument("--run-id", help="override run id; use only when intentionally reproducing a run")
    parser.add_argument("--poll-seconds", type=int, default=45)
    parser.add_argument("--http-timeout", type=float, default=12.0)
    parser.add_argument("--with-media", action=argparse.BooleanOptionalAction, default=True)
    parser.add_argument("--user-agent", default=BROWSER_UA, help="User-Agent for presigned object-storage requests")
    return parser.parse_args()


if __name__ == "__main__":
    sys.exit(Runner(parse_args()).run())
