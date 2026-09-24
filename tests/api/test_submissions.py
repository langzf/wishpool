import datetime as dt
import sys
import uuid

sys.path.insert(0, 'tests/api')
from conftest import api, get_auth_token, get_family_id, get_child_id


INTERNAL_TOKEN = "wishpool-local-internal-token"
MISSING_ID = "00000000-0000-0000-0000-000000000000"


def _create_manual_task(require_review=True):
    token = get_auth_token()
    today = dt.date.today()
    unique = uuid.uuid4().hex[:10]
    payload = {
        "familyId": get_family_id(),
        "childId": get_child_id(),
        "weekId": f"api-sub-{unique}",
        "startDate": today.isoformat(),
        "endDate": today.isoformat(),
        "rewardMode": "flexible",
        "rules": [
            {
                "title": f"Manual submission task {unique}",
                "category": "habit",
                "submissionType": "manual",
                "weekdays": [today.isoweekday()],
                "isCore": True,
                "requireReview": require_review,
                "sortOrder": 0,
            }
        ],
    }
    _, plan = api("POST", "/plans", payload, token=token, expect=200)
    api(
        "POST",
        "/internal/workflows/materialize-weekly-plan",
        {"weeklyPlanId": plan["id"]},
        headers={"X-Internal-Token": INTERNAL_TOKEN},
        expect=200,
    )
    _, today_body = api(
        "GET",
        f"/children/{get_child_id()}/today?date={today.isoformat()}",
        token=token,
        expect=200,
    )
    for task in today_body.get("tasks") or []:
        if task.get("title") == payload["rules"][0]["title"]:
            return task
    raise AssertionError(f"Materialized task not found in today snapshot: {today_body!r}")


def _create_submission():
    task = _create_manual_task()
    payload = {
        "taskInstanceId": task["id"],
        "clientMutationId": f"api-sub-{uuid.uuid4()}",
        "mediaAssetIds": [],
    }
    _, body = api("POST", "/submissions", payload, token=get_auth_token(), expect=201)
    assert body.get("id"), f"POST /submissions did not return id: {body!r}"
    return body


def test_create_submission_happy_path():
    submission = _create_submission()
    assert submission["status"] == "review_pending"
    assert submission["submissionType"] == "manual"


def test_create_submission_invalid_params_400():
    payload = {
        "taskInstanceId": MISSING_ID,
        "clientMutationId": "",
        "mediaAssetIds": [],
    }
    api("POST", "/submissions", payload, token=get_auth_token(), expect=400)


def test_create_submission_unauthenticated_401():
    task = _create_manual_task()
    payload = {
        "taskInstanceId": task["id"],
        "clientMutationId": f"api-sub-{uuid.uuid4()}",
        "mediaAssetIds": [],
    }
    api("POST", "/submissions", payload, expect=401)


def test_create_submission_not_found_404():
    payload = {
        "taskInstanceId": MISSING_ID,
        "clientMutationId": f"api-sub-{uuid.uuid4()}",
        "mediaAssetIds": [],
    }
    api("POST", "/submissions", payload, token=get_auth_token(), expect=404)


def test_get_submission_happy_path():
    submission = _create_submission()
    _, body = api("GET", f"/submissions/{submission['id']}", token=get_auth_token(), expect=200)
    assert body.get("id") == submission["id"]
    assert body.get("task", {}).get("id") == submission["taskInstanceId"]


def test_get_submission_invalid_params_400():
    api("GET", "/submissions/not-a-uuid", token=get_auth_token(), expect=400)


def test_get_submission_unauthenticated_401():
    submission = _create_submission()
    api("GET", f"/submissions/{submission['id']}", expect=401)


def test_get_submission_not_found_404():
    api("GET", f"/submissions/{MISSING_ID}", token=get_auth_token(), expect=404)


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
