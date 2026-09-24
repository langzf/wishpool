import sys
sys.path.insert(0, 'tests/api')
from conftest import api, get_auth_token, get_family_id, get_child_id


MISSING_UUID = "00000000-0000-0000-0000-000000000000"


def _family():
    token = get_auth_token()
    family_id = get_family_id(token)
    return token, family_id


def test_sync_pull_happy():
    token, family_id = _family()
    _, body = api("GET", f"/sync/pull?familyId={family_id}&afterSeq=0&limit=10", token=token, expect=200)
    assert isinstance(body.get("events"), list), f"events is not a list: {body!r}"
    assert "latestSeq" in body, f"missing latestSeq: {body!r}"


def test_sync_pull_bad_request():
    token, family_id = _family()
    status, _ = api("GET", f"/sync/pull?familyId={family_id}&afterSeq=-1", token=token)
    assert status == 400, f"expected HTTP 400, got {status}"


def test_sync_pull_unauthenticated():
    token, family_id = _family()
    status, _ = api("GET", f"/sync/pull?familyId={family_id}&afterSeq=0")
    assert status == 401, f"expected HTTP 401, got {status}"


def test_sync_pull_not_found_or_forbidden():
    token = get_auth_token()
    status, _ = api("GET", f"/sync/pull?familyId={MISSING_UUID}&afterSeq=0", token=token)
    assert status in (403, 404), f"expected HTTP 403 or 404 for inaccessible missing family, got {status}"


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
