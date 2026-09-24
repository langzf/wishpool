import datetime as dt
import sys
import uuid

sys.path.insert(0, 'tests/api')
from conftest import api, get_auth_token, get_family_id, get_child_id


INTERNAL_TOKEN = "wishpool-local-internal-token"
MISSING_ID = "00000000-0000-0000-0000-000000000000"


def _today_plan_payload():
    today = dt.date.today()
    unique = uuid.uuid4().hex[:10]
    return {
        "familyId": get_family_id(),
        "childId": get_child_id(),
        "weekId": f"api-plan-{unique}",
        "startDate": today.isoformat(),
        "endDate": today.isoformat(),
        "rewardMode": "flexible",
        "rules": [
            {
                "title": f"Manual plan task {unique}",
                "category": "habit",
                "submissionType": "manual",
                "description": "API test task",
                "targetText": "Done",
                "weekdays": [today.isoweekday()],
                "isCore": True,
                "requireReview": True,
                "sortOrder": 0,
            }
        ],
    }


def _create_plan():
    _, body = api("POST", "/plans", _today_plan_payload(), token=get_auth_token(), expect=200)
    assert body.get("id"), f"POST /plans did not return id: {body!r}"
    return body


def test_create_plan_happy_path():
    plan = _create_plan()
    assert plan["status"] == "active"
    assert len(plan.get("rules") or []) == 1


def test_create_plan_invalid_params_400():
    payload = _today_plan_payload()
    payload["rewardMode"] = "bad-mode"
    api("POST", "/plans", payload, token=get_auth_token(), expect=400)


def test_create_plan_unauthenticated_401():
    api("POST", "/plans", _today_plan_payload(), expect=401)


def test_get_plan_happy_path():
    plan = _create_plan()
    _, body = api("GET", f"/plans/{plan['id']}", token=get_auth_token(), expect=200)
    assert body.get("id") == plan["id"]


def test_get_plan_invalid_params_400():
    api("GET", "/plans/not-a-uuid", token=get_auth_token(), expect=400)


def test_get_plan_unauthenticated_401():
    plan = _create_plan()
    api("GET", f"/plans/{plan['id']}", expect=401)


def test_get_plan_not_found_404():
    api("GET", f"/plans/{MISSING_ID}", token=get_auth_token(), expect=404)


def test_list_task_templates_happy_path():
    _, body = api("GET", f"/task-templates?familyId={get_family_id()}", token=get_auth_token(), expect=200)
    assert isinstance(body, list)


def test_list_task_templates_missing_params_400():
    api("GET", "/task-templates", token=get_auth_token(), expect=400)


def test_list_task_templates_unauthenticated_401():
    api("GET", f"/task-templates?familyId={get_family_id()}", expect=401)


def test_create_task_template_happy_path():
    payload = {
        "familyId": get_family_id(),
        "title": f"Template {uuid.uuid4().hex[:8]}",
        "category": "reading",
        "submissionType": "manual",
        "description": "API template",
        "targetText": "Read",
        "defaultDurationSec": 600,
    }
    _, body = api("POST", "/task-templates", payload, token=get_auth_token(), expect=201)
    assert body.get("id")
    assert body.get("title") == payload["title"]


def test_create_task_template_invalid_params_400():
    payload = {
        "familyId": get_family_id(),
        "title": "Bad template",
        "category": "unsupported",
        "submissionType": "manual",
    }
    api("POST", "/task-templates", payload, token=get_auth_token(), expect=400)


def test_create_task_template_unauthenticated_401():
    payload = {
        "familyId": get_family_id(),
        "title": "Unauthorized template",
        "category": "habit",
        "submissionType": "manual",
    }
    api("POST", "/task-templates", payload, expect=401)


def test_plan_materializes_task_instances_happy_path():
    plan = _create_plan()
    api(
        "POST",
        "/internal/workflows/materialize-weekly-plan",
        {"weeklyPlanId": plan["id"]},
        headers={"X-Internal-Token": INTERNAL_TOKEN},
        expect=200,
    )
    _, body = api(
        "GET",
        f"/children/{get_child_id()}/today?date={plan['startDate']}",
        token=get_auth_token(),
        expect=200,
    )
    assert any(task.get("title") == plan["rules"][0]["title"] for task in body.get("tasks") or [])


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
