import datetime as dt
import sys
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


def _new_child():
    _, child = api(
        "POST",
        f"/families/{get_family_id()}/children",
        {"nickname": f"API child {uuid.uuid4().hex[:6]}", "birthYear": 2017, "roomTheme": "forest"},
        token=get_auth_token(),
        expect=201,
    )
    return child


def _wish_payload(child_id=None, required_fragments=3):
    unique = uuid.uuid4().hex[:10]
    return {
        "familyId": get_family_id(),
        "childId": child_id or get_child_id(),
        "weekId": f"api-wish-{unique}",
        "title": f"Wish {unique}",
        "note": "API test wish",
        "requiredFragments": required_fragments,
        "rewardMode": "flexible",
    }


def _create_wish(child_id=None, required_fragments=3):
    _, body = api("POST", "/wishes", _wish_payload(child_id, required_fragments), token=get_auth_token(), expect=201)
    assert body.get("id"), f"POST /wishes did not return id: {body!r}"
    return body


def _activate_wish(wish_id):
    _, body = api("POST", f"/wishes/{wish_id}/activate", {}, token=get_auth_token(), expect=200)
    return body


def _create_manual_task_for_wish(wish):
    today = dt.date.today()
    unique = uuid.uuid4().hex[:10]
    plan_payload = {
        "familyId": get_family_id(),
        "childId": wish["childId"],
        "weekId": wish["weekId"],
        "startDate": today.isoformat(),
        "endDate": today.isoformat(),
        "rewardMode": "flexible",
        "wishId": wish["id"],
        "rules": [
            {
                "title": f"Wish unlock task {unique}",
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
    _, today_body = api(
        "GET",
        f"/children/{wish['childId']}/today?date={today.isoformat()}",
        token=get_auth_token(),
        expect=200,
    )
    task = next((item for item in today_body.get("tasks") or [] if item.get("title") == plan_payload["rules"][0]["title"]), None)
    assert task, f"Materialized task not found: {today_body!r}"
    return task


def _approve_task(task):
    _, submission = api(
        "POST",
        "/submissions",
        {"taskInstanceId": task["id"], "clientMutationId": f"api-wish-{uuid.uuid4()}", "mediaAssetIds": []},
        token=get_auth_token(),
        expect=201,
    )
    _, review = api(
        "POST",
        "/reviews",
        {"submissionId": submission["id"], "decision": "approved"},
        token=get_auth_token(),
        expect=201,
    )
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


def _upload_redemption_photo(wish):
    _, session = api(
        "POST",
        "/media/upload-sessions",
        {
            "familyId": wish["familyId"],
            "childId": wish["childId"],
            "purpose": "wish_redemption",
            "contentType": "image/png",
            "sizeBytes": len(PNG_1X1),
            "relatedResource": {"type": "wish", "id": wish["id"]},
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


def _upload_wish_image(family_id, child_id):
    _, session = api(
        "POST",
        "/media/upload-sessions",
        {
            "familyId": family_id,
            "childId": child_id,
            "purpose": "wish_image",
            "contentType": "image/png",
            "sizeBytes": len(PNG_1X1),
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
    request = urllib.request.Request(target_url, data=PNG_1X1, headers=headers, method="PUT")
    with urllib.request.urlopen(request, timeout=10) as response:
        assert 200 <= response.status < 300
    _, media = api(
        "POST",
        f"/media/{session['mediaId']}/finalize",
        {"width": 1, "height": 1},
        token=get_auth_token(),
        expect=200,
    )
    return media["id"]


def _create_unlocked_wish():
    wish = _activate_wish(_create_wish(_new_child()["id"], required_fragments=1)["id"])
    task = _create_manual_task_for_wish(wish)
    _approve_task(task)
    _, unlocked = api("GET", f"/wishes/{wish['id']}", token=get_auth_token(), expect=200)
    assert unlocked["status"] == "unlocked", f"Wish did not unlock: {unlocked!r}"
    return unlocked


def test_create_wish_happy_path():
    wish = _create_wish()
    assert wish["status"] == "draft"


def test_attach_wish_image_updates_wish_and_returns_media():
    wish = _create_wish()
    media_id = _upload_wish_image(wish["familyId"], wish["childId"])
    _, attached = api(
        "POST",
        f"/wishes/{wish['id']}/image",
        {"mediaId": media_id, "sourceType": "generated"},
        token=get_auth_token(),
        expect=200,
    )
    assert attached["imageMedia"]["id"] == media_id

    _, listed = api(
        "GET",
        f"/children/{wish['childId']}/wishes?weekId={wish['weekId']}",
        token=get_auth_token(),
        expect=200,
    )
    listed_wish = next(item for item in listed if item["id"] == wish["id"])
    assert listed_wish["imageMedia"]["id"] == media_id


def test_create_wish_invalid_params_400():
    payload = _wish_payload()
    payload["title"] = " "
    api("POST", "/wishes", payload, token=get_auth_token(), expect=400)


def test_create_wish_unauthenticated_401():
    api("POST", "/wishes", _wish_payload(), expect=401)


def test_get_wish_happy_path():
    wish = _create_wish()
    _, body = api("GET", f"/wishes/{wish['id']}", token=get_auth_token(), expect=200)
    assert body.get("id") == wish["id"]


def test_get_wish_invalid_params_400():
    api("GET", "/wishes/not-a-uuid", token=get_auth_token(), expect=400)


def test_get_wish_unauthenticated_401():
    wish = _create_wish()
    api("GET", f"/wishes/{wish['id']}", expect=401)


def test_get_wish_not_found_404():
    api("GET", f"/wishes/{MISSING_ID}", token=get_auth_token(), expect=404)


def test_activate_wish_happy_path():
    wish = _create_wish()
    active = _activate_wish(wish["id"])
    assert active["status"] == "active"


def test_activate_wish_invalid_params_400():
    api("POST", "/wishes/not-a-uuid/activate", {}, token=get_auth_token(), expect=400)


def test_activate_wish_unauthenticated_401():
    wish = _create_wish()
    api("POST", f"/wishes/{wish['id']}/activate", {}, expect=401)


def test_activate_wish_not_found_404():
    api("POST", f"/wishes/{MISSING_ID}/activate", {}, token=get_auth_token(), expect=404)


def test_redeem_wish_happy_path():
    wish = _create_unlocked_wish()
    photo_id = _upload_redemption_photo(wish)
    _, body = api(
        "POST",
        f"/wishes/{wish['id']}/redeem",
        {"redeemedDate": dt.date.today().isoformat(), "photoMediaIds": [photo_id], "parentNote": "Redeemed"},
        token=get_auth_token(),
        expect=200,
    )
    assert body.get("wishId") == wish["id"]


def test_redeem_wish_invalid_params_400():
    wish = _create_wish()
    api(
        "POST",
        f"/wishes/{wish['id']}/redeem",
        {"redeemedDate": dt.date.today().isoformat(), "photoMediaIds": []},
        token=get_auth_token(),
        expect=400,
    )


def test_redeem_wish_unauthenticated_401():
    wish = _create_wish()
    api("POST", f"/wishes/{wish['id']}/redeem", {"redeemedDate": dt.date.today().isoformat(), "photoMediaIds": [MISSING_ID]}, expect=401)


def test_redeem_wish_not_found_404():
    api(
        "POST",
        f"/wishes/{MISSING_ID}/redeem",
        {"redeemedDate": dt.date.today().isoformat(), "photoMediaIds": [MISSING_ID]},
        token=get_auth_token(),
        expect=404,
    )


def test_list_child_wishes_happy_path():
    wish = _create_wish()
    _, body = api("GET", f"/children/{wish['childId']}/wishes?weekId={wish['weekId']}", token=get_auth_token(), expect=200)
    assert any(item.get("id") == wish["id"] for item in body)


def test_list_child_wishes_invalid_params_400():
    api("GET", "/children/not-a-uuid/wishes", token=get_auth_token(), expect=400)


def test_list_child_wishes_unauthenticated_401():
    api("GET", f"/children/{get_child_id()}/wishes", expect=401)


def test_get_current_wish_happy_path():
    child = _new_child()
    wish = _activate_wish(_create_wish(child["id"])["id"])
    _, body = api("GET", f"/children/{child['id']}/wishes/current", token=get_auth_token(), expect=200)
    assert body.get("id") == wish["id"]


def test_get_current_wish_invalid_params_400():
    api("GET", "/children/not-a-uuid/wishes/current", token=get_auth_token(), expect=400)


def test_get_current_wish_unauthenticated_401():
    api("GET", f"/children/{get_child_id()}/wishes/current", expect=401)


def test_get_current_wish_not_found_404():
    child = _new_child()
    api("GET", f"/children/{child['id']}/wishes/current", token=get_auth_token(), expect=404)


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
