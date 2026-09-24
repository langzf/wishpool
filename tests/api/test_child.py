import sys
sys.path.insert(0, "tests/api")
from conftest import api, get_auth_token, get_family_id, get_child_id


MISSING_UUID = "00000000-0000-0000-0000-000000000000"


def test_update_child_happy_path():
    token = get_auth_token()
    family_id = get_family_id(token)
    child_id = get_child_id(token, family_id)
    _, body = api(
        "PATCH",
        f"/children/{child_id}",
        {"nickname": "Updated API Child", "birthYear": 2017, "roomTheme": "ocean"},
        token=token,
        expect=200,
    )
    assert body.get("id") == child_id, f"child id mismatch: {body!r}"
    assert body.get("nickname") == "Updated API Child", f"nickname mismatch: {body!r}"


def test_update_child_missing_params_happy_path():
    token = get_auth_token()
    family_id = get_family_id(token)
    child_id = get_child_id(token, family_id)
    _, body = api("PATCH", f"/children/{child_id}", {}, token=token, expect=200)
    assert body.get("id") == child_id, f"child id mismatch: {body!r}"


def test_update_child_invalid_params_400():
    token = get_auth_token()
    family_id = get_family_id(token)
    child_id = get_child_id(token, family_id)
    api("PATCH", f"/children/{child_id}", {"birthYear": 1999}, token=token, expect=400)


def test_update_child_invalid_path_400():
    token = get_auth_token()
    api("PATCH", "/children/not-a-uuid", {"nickname": "Bad Path"}, token=token, expect=400)


def test_update_child_unauthenticated_401():
    api("PATCH", f"/children/{MISSING_UUID}", {"nickname": "No Auth"}, expect=401)


def test_get_child_home_context_happy_path():
    token = get_auth_token()
    family_id = get_family_id(token)
    child_id = get_child_id(token, family_id)
    _, body = api("GET", f"/children/{child_id}/home-context", token=token, expect=200)
    assert body["child"]["id"] == child_id, f"child id mismatch: {body!r}"
    assert "today" in body, f"today missing: {body!r}"


def test_get_child_home_context_invalid_params_400():
    token = get_auth_token()
    family_id = get_family_id(token)
    child_id = get_child_id(token, family_id)
    api("GET", f"/children/{child_id}/home-context?date=not-a-date", token=token, expect=400)


def test_get_child_home_context_invalid_path_400():
    token = get_auth_token()
    api("GET", "/children/not-a-uuid/home-context", token=token, expect=400)


def test_get_child_home_context_unauthenticated_401():
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
