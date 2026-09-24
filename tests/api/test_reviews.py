import datetime as dt
import sys
import uuid

sys.path.insert(0, 'tests/api')
from conftest import api, get_auth_token, get_family_id, get_child_id


INTERNAL_TOKEN = "wishpool-local-internal-token"
MISSING_ID = "00000000-0000-0000-0000-000000000000"


def _create_pending_submission():
    token = get_auth_token()
    today = dt.date.today()
    unique = uuid.uuid4().hex[:10]
    plan_payload = {
        "familyId": get_family_id(),
        "childId": get_child_id(),
        "weekId": f"api-rev-{unique}",
        "startDate": today.isoformat(),
        "endDate": today.isoformat(),
        "rewardMode": "flexible",
        "rules": [
            {
                "title": f"Review task {unique}",
                "category": "habit",
                "submissionType": "manual",
                "weekdays": [today.isoweekday()],
                "isCore": True,
                "requireReview": True,
                "sortOrder": 0,
            }
        ],
    }
    _, plan = api("POST", "/plans", plan_payload, token=token, expect=200)
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
    task = next((item for item in today_body.get("tasks") or [] if item.get("title") == plan_payload["rules"][0]["title"]), None)
    assert task, f"Materialized task not found: {today_body!r}"
    _, submission = api(
        "POST",
        "/submissions",
        {
            "taskInstanceId": task["id"],
            "clientMutationId": f"api-review-{uuid.uuid4()}",
            "mediaAssetIds": [],
        },
        token=token,
        expect=201,
    )
    return submission


def _create_review(decision="approved"):
    submission = _create_pending_submission()
    _, review = api(
        "POST",
        "/reviews",
        {
            "submissionId": submission["id"],
            "decision": decision,
            "feedback": {"emoji": "ok", "text": "Looks good"},
        },
        token=get_auth_token(),
        expect=201,
    )
    assert review.get("id"), f"POST /reviews did not return id: {review!r}"
    return review


def test_create_review_happy_path():
    review = _create_review()
    assert review["decision"] == "approved"


def test_create_review_invalid_params_400():
    api(
        "POST",
        "/reviews",
        {"submissionId": MISSING_ID, "decision": "bad-decision"},
        token=get_auth_token(),
        expect=400,
    )


def test_create_review_unauthenticated_401():
    submission = _create_pending_submission()
    api("POST", "/reviews", {"submissionId": submission["id"], "decision": "approved"}, expect=401)


def test_create_review_not_found_404():
    api("POST", "/reviews", {"submissionId": MISSING_ID, "decision": "approved"}, token=get_auth_token(), expect=404)


def test_list_pending_reviews_happy_path():
    submission = _create_pending_submission()
    _, body = api("GET", f"/reviews/pending?familyId={get_family_id()}", token=get_auth_token(), expect=200)
    assert isinstance(body, list)
    assert any(item.get("submission", {}).get("id") == submission["id"] for item in body)


def test_list_pending_reviews_missing_params_400():
    api("GET", "/reviews/pending", token=get_auth_token(), expect=400)


def test_list_pending_reviews_unauthenticated_401():
    api("GET", f"/reviews/pending?familyId={get_family_id()}", expect=401)


def test_get_review_detail_happy_path():
    submission = _create_pending_submission()
    _, body = api("GET", f"/reviews/{submission['id']}/detail", token=get_auth_token(), expect=200)
    assert body.get("id") == submission["id"]


def test_get_review_detail_invalid_params_400():
    api("GET", "/reviews/not-a-uuid/detail", token=get_auth_token(), expect=400)


def test_get_review_detail_unauthenticated_401():
    submission = _create_pending_submission()
    api("GET", f"/reviews/{submission['id']}/detail", expect=401)


def test_get_review_detail_not_found_404():
    api("GET", f"/reviews/{MISSING_ID}/detail", token=get_auth_token(), expect=404)


def test_revoke_review_happy_path():
    review = _create_review()
    _, body = api("POST", f"/reviews/{review['id']}/revoke", {"reason": "API test revoke"}, token=get_auth_token(), expect=200)
    assert body.get("id") == review["id"]


def test_revoke_review_invalid_params_400():
    review = _create_review()
    api("POST", f"/reviews/{review['id']}/revoke", {"reason": " "}, token=get_auth_token(), expect=400)


def test_revoke_review_unauthenticated_401():
    review = _create_review()
    api("POST", f"/reviews/{review['id']}/revoke", {"reason": "No auth"}, expect=401)


def test_revoke_review_not_found_404():
    api("POST", f"/reviews/{MISSING_ID}/revoke", {"reason": "Missing"}, token=get_auth_token(), expect=404)


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
