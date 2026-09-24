import sys
sys.path.insert(0, "tests/api")
from conftest import api, get_auth_token, get_family_id, get_child_id


MISSING_UUID = "00000000-0000-0000-0000-000000000000"


def test_parent_dashboard_happy_path():
    token = get_auth_token()
    family_id = get_family_id(token)
    child_id = get_child_id(token, family_id)
    _, body = api(
        "GET",
        f"/families/{family_id}/parent-dashboard?childId={child_id}",
        token=token,
        expect=200,
    )
    assert body["family"]["id"] == family_id, f"family id mismatch: {body!r}"
    assert body["selectedChild"]["id"] == child_id, f"selected child mismatch: {body!r}"


def test_parent_dashboard_missing_optional_params_happy_path():
    token = get_auth_token()
    family_id = get_family_id(token)
    _, body = api("GET", f"/families/{family_id}/parent-dashboard", token=token, expect=200)
    assert body["family"]["id"] == family_id, f"family id mismatch: {body!r}"
    assert isinstance(body.get("children"), list), f"children must be a list: {body!r}"


def test_parent_dashboard_invalid_params_400():
    token = get_auth_token()
    family_id = get_family_id(token)
    api("GET", f"/families/{family_id}/parent-dashboard?date=not-a-date", token=token, expect=400)


def test_parent_dashboard_invalid_child_param_400():
    token = get_auth_token()
    family_id = get_family_id(token)
    get_child_id(token, family_id)
    api(
        "GET",
        f"/families/{family_id}/parent-dashboard?childId={MISSING_UUID}",
        token=token,
        expect=400,
    )


def test_parent_dashboard_invalid_path_400():
    token = get_auth_token()
    api("GET", "/families/not-a-uuid/parent-dashboard", token=token, expect=400)


def test_parent_dashboard_unauthenticated_401():
    api("GET", f"/families/{MISSING_UUID}/parent-dashboard", expect=401)


def test_child_home_context_happy_path():
    token = get_auth_token()
    family_id = get_family_id(token)
    child_id = get_child_id(token, family_id)
    _, body = api("GET", f"/children/{child_id}/home-context", token=token, expect=200)
    assert body["child"]["id"] == child_id, f"child id mismatch: {body!r}"
    assert body.get("room"), f"room missing: {body!r}"


def test_child_home_context_invalid_params_400():
    token = get_auth_token()
    family_id = get_family_id(token)
    child_id = get_child_id(token, family_id)
    api("GET", f"/children/{child_id}/home-context?date=bad-date", token=token, expect=400)


def test_child_home_context_invalid_path_400():
    token = get_auth_token()
    api("GET", "/children/not-a-uuid/home-context", token=token, expect=400)


def test_child_home_context_unauthenticated_401():
    api("GET", f"/children/{MISSING_UUID}/home-context", expect=401)


def _run_all():
    tests = [(name, value) for name, value in globals().items() if name.startswith("test_") and callable(value)]
    passed = 0
    for name, test in tests:
        try:
            test()
            print(f"PASS {name}")
            passed += 1
        except Exception as error:
            print(f"FAIL {name}: {error}")
    print(f"TOTAL {passed}/{len(tests)}")
    sys.exit(0 if passed == len(tests) else 1)


if __name__ == "__main__":
    _run_all()
