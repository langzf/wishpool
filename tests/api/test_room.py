import sys
sys.path.insert(0, 'tests/api')
from conftest import api, get_auth_token, get_family_id, get_child_id


MISSING_UUID = "00000000-0000-0000-0000-000000000000"


def _child():
    token = get_auth_token()
    family_id = get_family_id(token)
    child_id = get_child_id(token, family_id)
    return token, child_id


def test_room_state_happy():
    token, child_id = _child()
    _, body = api("GET", f"/room/state?childId={child_id}", token=token, expect=200)
    assert body["childId"] == child_id
    assert isinstance(body.get("items"), list), f"items is not a list: {body!r}"


def test_room_state_unauthenticated():
    token, child_id = _child()
    status, _ = api("GET", f"/room/state?childId={child_id}")
    assert status == 401, f"expected HTTP 401, got {status}"


def test_room_state_not_found():
    token = get_auth_token()
    status, _ = api("GET", f"/room/state?childId={MISSING_UUID}", token=token)
    assert status in (403, 404), f"expected HTTP 403 or 404 for inaccessible missing child, got {status}"


def test_arrange_room_item_happy_when_item_exists():
    token, child_id = _child()
    _, state = api("GET", f"/room/state?childId={child_id}", token=token, expect=200)
    items = state.get("items") or []
    if not items:
        return
    item_id = items[0]["id"]
    _, body = api(
        "POST",
        f"/room/items/{item_id}/arrange",
        {"position": {"x": 1, "y": 2, "scale": 1}},
        token=token,
        expect=200,
    )
    assert body["id"] == item_id


def test_arrange_room_item_bad_request():
    token, _ = _child()
    status, _ = api("POST", f"/room/items/{MISSING_UUID}/arrange", {"position": {}}, token=token)
    assert status == 400, f"expected HTTP 400, got {status}"


def test_arrange_room_item_unauthenticated():
    token, child_id = _child()
    _, state = api("GET", f"/room/state?childId={child_id}", token=token, expect=200)
    items = state.get("items") or []
    if not items:
        return
    status, _ = api("POST", f"/room/items/{items[0]['id']}/arrange", {"position": {"x": 0}})
    assert status == 401, f"expected HTTP 401, got {status}"


def test_arrange_room_item_not_found():
    token, _ = _child()
    status, _ = api("POST", f"/room/items/{MISSING_UUID}/arrange", {"position": {"x": 0}}, token=token)
    assert status == 404, f"expected HTTP 404, got {status}"


def _run():
    tests = [(name, obj) for name, obj in sorted(globals().items()) if name.startswith("test_") and callable(obj)]
    passed = 0
    for name, test in tests:
        try:
            test()
            passed += 1
            print(f"PASS {name}")
        except Exception as error:
            print(f"FAIL {name}: {error}")
    print(f"TOTAL {passed}/{len(tests)}")
    sys.exit(0 if passed == len(tests) else 1)


if __name__ == "__main__":
    _run()
