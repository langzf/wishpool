import datetime as dt
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

sys.path.insert(0, 'tests/api')
from conftest import api, get_auth_token, get_family_id, get_child_id


INTERNAL_TOKEN = "wishpool-local-internal-token"
MISSING_ID = "00000000-0000-0000-0000-000000000000"
PNG_1X1 = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01"
    b"\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\x00\x01"
    b"\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82"
)


def _upload_png(family_id, child_id, purpose, related_type, related_id):
    _, session = api(
        "POST",
        "/media/upload-sessions",
        {
            "familyId": family_id,
            "childId": child_id,
            "purpose": purpose,
            "contentType": "image/png",
            "sizeBytes": len(PNG_1X1),
            "relatedResource": {"type": related_type, "id": related_id},
        },
        token=get_auth_token(),
        expect=201,
    )
    headers = {"Content-Type": "image/png", "Content-Length": str(len(PNG_1X1))}
    target_url = session["uploadUrl"]
    parsed = urllib.parse.urlsplit(target_url)
    if parsed.hostname == "minio":
        port = f":{parsed.port}" if parsed.port else ""
        target_url = urllib.parse.urlunsplit(
            (parsed.scheme, f"localhost{port}", parsed.path, parsed.query, parsed.fragment)
        )
        headers["Host"] = parsed.netloc
    request = urllib.request.Request(
        target_url,
        data=PNG_1X1,
        headers=headers,
        method="PUT",
    )
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            assert 200 <= response.status < 300, f"PUT upload URL returned {response.status}"
    except urllib.error.HTTPError as error:
        raise AssertionError(f"upload PUT got HTTP {error.code}: {error.read()!r}") from error
    _, media = api("POST", f"/media/{session['mediaId']}/finalize", {"width": 1, "height": 1}, token=get_auth_token(), expect=200)
    return media["id"]


def _new_child():
    _, child = api(
        "POST",
        f"/families/{get_family_id()}/children",
        {"nickname": f"API memory child {uuid.uuid4().hex[:6]}", "birthYear": 2017, "roomTheme": "forest"},
        token=get_auth_token(),
        expect=201,
    )
    return child


def _create_wish(required_fragments=1):
    unique = uuid.uuid4().hex[:10]
    child = _new_child()
    _, wish = api(
        "POST",
        "/wishes",
        {
            "familyId": get_family_id(),
            "childId": child["id"],
            "weekId": f"api-memory-{unique}",
            "title": f"Memory wish {unique}",
            "note": "API memory setup",
            "requiredFragments": required_fragments,
            "rewardMode": "flexible",
        },
        token=get_auth_token(),
        expect=201,
    )
    _, active = api("POST", f"/wishes/{wish['id']}/activate", {}, token=get_auth_token(), expect=200)
    return active


def _create_task_for_wish(wish):
    today = dt.date.today()
    unique = uuid.uuid4().hex[:10]
    plan_payload = {
        "familyId": wish["familyId"],
        "childId": wish["childId"],
        "weekId": wish["weekId"],
        "startDate": today.isoformat(),
        "endDate": today.isoformat(),
        "rewardMode": "flexible",
        "wishId": wish["id"],
        "rules": [
            {
                "title": f"Memory task {unique}",
                "category": "habit",
                "submissionType": "manual",
                "weekdays": [today.isoweekday()],
                "isCore": True,
                "requireReview": True,
                "sortOrder": 0,
            }
        ],
    }
    _, plan = api("POST", "/plans", plan_payload, token=get_auth_token(), expect=200)
    api(
        "POST",
        "/internal/workflows/materialize-weekly-plan",
        {"weeklyPlanId": plan["id"]},
        headers={"X-Internal-Token": INTERNAL_TOKEN},
        expect=200,
    )
    _, today_body = api("GET", f"/children/{wish['childId']}/today?date={today.isoformat()}", token=get_auth_token(), expect=200)
    task = next((item for item in today_body.get("tasks") or [] if item.get("title") == plan_payload["rules"][0]["title"]), None)
    assert task, f"Materialized memory task not found: {today_body!r}"
    return task


def _approve_task(task):
    _, submission = api(
        "POST",
        "/submissions",
        {"taskInstanceId": task["id"], "clientMutationId": f"api-memory-{uuid.uuid4()}", "mediaAssetIds": []},
        token=get_auth_token(),
        expect=201,
    )
    _, review = api("POST", "/reviews", {"submissionId": submission["id"], "decision": "approved"}, token=get_auth_token(), expect=201)
    api(
        "POST",
        "/internal/workflows/evaluate-reward",
        {
            "eventType": "review.approved",
            "taskInstanceId": task["id"],
            "reviewId": review["id"],
            "actorUserId": review["reviewedBy"],
        },
        headers={"X-Internal-Token": INTERNAL_TOKEN},
        expect=200,
    )


def _find_memory(child_id, week_id):
    _, body = api("GET", f"/memories?childId={child_id}&limit=50", token=get_auth_token(), expect=200)
    for item in body.get("items") or []:
        if item.get("weekId") == week_id:
            return item
    return None


def _claim_redeemed_event(wish_id):
    _, claim = api(
        "POST",
        "/internal/outbox/events/claim",
        {"limit": 500, "leaseSeconds": 5},
        headers={"X-Internal-Token": INTERNAL_TOKEN},
        expect=200,
    )
    for event in claim.get("events") or []:
        payload = event.get("payload") or {}
        if event.get("type") == "wish.redeemed" and payload.get("wishId") == wish_id:
            return event["id"]
    return None


def _create_memory():
    wish = _create_wish()
    _approve_task(_create_task_for_wish(wish))
    _, unlocked = api("GET", f"/wishes/{wish['id']}", token=get_auth_token(), expect=200)
    assert unlocked["status"] == "unlocked", f"Wish did not unlock: {unlocked!r}"
    photo_id = _upload_png(wish["familyId"], wish["childId"], "wish_redemption", "wish", wish["id"])
    api(
        "POST",
        f"/wishes/{wish['id']}/redeem",
        {"redeemedDate": dt.date.today().isoformat(), "photoMediaIds": [photo_id]},
        token=get_auth_token(),
        expect=200,
    )

    memory = None
    for _ in range(5):
        memory = _find_memory(wish["childId"], wish["weekId"])
        if memory is not None:
            break
        time.sleep(0.2)
    if memory is None:
        event_id = _claim_redeemed_event(wish["id"])
        if event_id:
            api(
                "POST",
                "/internal/workflows/generate-memory",
                {"triggeredByEventId": event_id},
                headers={"X-Internal-Token": INTERNAL_TOKEN},
                expect=200,
            )
        for _ in range(5):
            memory = _find_memory(wish["childId"], wish["weekId"])
            if memory is not None:
                break
            time.sleep(0.2)
    assert memory, f"Generated memory not found for week {wish['weekId']}"
    return memory


def test_list_memories_happy_path():
    _, body = api("GET", f"/memories?childId={get_child_id()}&limit=10", token=get_auth_token(), expect=200)
    assert isinstance(body.get("items"), list)


def test_list_memories_missing_params_400():
    api("GET", "/memories", token=get_auth_token(), expect=400)


def test_list_memories_unauthenticated_401():
    api("GET", f"/memories?childId={get_child_id()}", expect=401)


def test_get_memory_happy_path():
    memory = _create_memory()
    _, body = api("GET", f"/memories/{memory['id']}", token=get_auth_token(), expect=200)
    assert body.get("id") == memory["id"]


def test_get_memory_invalid_params_400():
    api("GET", "/memories/not-a-uuid", token=get_auth_token(), expect=400)


def test_get_memory_unauthenticated_401():
    memory = _create_memory()
    api("GET", f"/memories/{memory['id']}", expect=401)


def test_get_memory_not_found_404():
    api("GET", f"/memories/{MISSING_ID}", token=get_auth_token(), expect=404)


def test_export_memory_happy_path():
    memory = _create_memory()
    _, body = api("POST", f"/memories/{memory['id']}/export", {"format": "pdf"}, token=get_auth_token(), expect=200)
    assert body.get("status") == "requested"


def test_export_memory_invalid_params_400():
    memory = _create_memory()
    api("POST", f"/memories/{memory['id']}/export", {"format": "docx"}, token=get_auth_token(), expect=400)


def test_export_memory_unauthenticated_401():
    memory = _create_memory()
    api("POST", f"/memories/{memory['id']}/export", {"format": "pdf"}, expect=401)


def test_export_memory_not_found_404():
    api("POST", f"/memories/{MISSING_ID}/export", {"format": "pdf"}, token=get_auth_token(), expect=404)


def _run_all():
    tests = sorted((name, fn) for name, fn in globals().items() if name.startswith("test_") and callable(fn))
    passed = 0
    for name, fn in tests:
        try:
            fn()
            print(f"PASS {name}")
            passed += 1
        except Exception as error:
            print(f"FAIL {name}: {error}")
    print(f"TOTAL {passed}/{len(tests)}")
    return 0 if passed == len(tests) else 1


if __name__ == "__main__":
    raise SystemExit(_run_all())
