import sys
sys.path.insert(0, "tests/api")
from conftest import api, get_auth_token, get_family_id, get_child_id

import uuid


MISSING_UUID = "00000000-0000-0000-0000-000000000000"


def test_create_family_happy_path():
    token = get_auth_token()
    _, body = api(
        "POST",
        "/families",
        {"name": f"Family {uuid.uuid4().hex[:8]}", "timezone": "Asia/Shanghai"},
        token=token,
        expect=201,
    )
    assert body.get("id"), f"id missing: {body!r}"
    assert body.get("timezone") == "Asia/Shanghai", f"timezone mismatch: {body!r}"


def test_create_family_missing_params_400():
    token = get_auth_token()
    api("POST", "/families", {"name": "Missing Timezone"}, token=token, expect=400)


def test_create_family_unauthenticated_401():
    api(
        "POST",
        "/families",
        {"name": "No Auth Family", "timezone": "Asia/Shanghai"},
        expect=401,
    )


def test_get_family_happy_path():
    token = get_auth_token()
    family_id = get_family_id(token)
    _, body = api("GET", f"/families/{family_id}", token=token, expect=200)
    assert body.get("id") == family_id, f"family id mismatch: {body!r}"


def test_get_family_invalid_params_400():
    token = get_auth_token()
    api("GET", "/families/not-a-uuid", token=token, expect=400)


def test_get_family_unauthenticated_401():
    api("GET", f"/families/{MISSING_UUID}", expect=401)


def test_list_members_happy_path():
    token = get_auth_token()
    family_id = get_family_id(token)
    _, body = api("GET", f"/families/{family_id}/members", token=token, expect=200)
    assert isinstance(body, list), f"members must be a list: {body!r}"
    assert body and body[0].get("familyId") == family_id, f"member family mismatch: {body!r}"


def test_list_members_invalid_params_400():
    token = get_auth_token()
    api("GET", "/families/not-a-uuid/members", token=token, expect=400)


def test_list_members_unauthenticated_401():
    api("GET", f"/families/{MISSING_UUID}/members", expect=401)


def test_invite_parent_happy_path():
    token = get_auth_token()
    family_id = get_family_id(token)
    _, body = api(
        "POST",
        f"/families/{family_id}/invites",
        {"contact": f"+1555{uuid.uuid4().int % 10_000_000:07d}"},
        token=token,
        expect=201,
    )
    assert body.get("id"), f"id missing: {body!r}"
    assert body.get("familyId") == family_id, f"family id mismatch: {body!r}"


def test_invite_parent_missing_params_400():
    token = get_auth_token()
    family_id = get_family_id(token)
    api("POST", f"/families/{family_id}/invites", {}, token=token, expect=400)


def test_invite_parent_invalid_params_400():
    token = get_auth_token()
    api("POST", "/families/not-a-uuid/invites", {"contact": "+15551234567"}, token=token, expect=400)


def test_invite_parent_unauthenticated_401():
    api("POST", f"/families/{MISSING_UUID}/invites", {"contact": "+15551234567"}, expect=401)


def test_create_family_child_happy_path():
    token = get_auth_token()
    family_id = get_family_id(token)
    child_id = get_child_id(token, family_id)
    assert child_id, "child id missing"


def test_create_family_child_missing_params_400():
    token = get_auth_token()
    family_id = get_family_id(token)
    api("POST", f"/families/{family_id}/children", {"birthYear": 2016}, token=token, expect=400)


def test_create_family_child_invalid_params_400():
    token = get_auth_token()
    family_id = get_family_id(token)
    api(
        "POST",
        f"/families/{family_id}/children",
        {"nickname": "Invalid Year", "birthYear": 1999},
        token=token,
        expect=400,
    )


def test_create_family_child_unauthenticated_401():
    api(
        "POST",
        f"/families/{MISSING_UUID}/children",
        {"nickname": "No Auth Child", "birthYear": 2016},
        expect=401,
    )


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
