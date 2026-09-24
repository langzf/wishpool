import sys
sys.path.insert(0, 'tests/api')
from conftest import api, get_auth_token, get_family_id, get_child_id


def _family():
    token = get_auth_token()
    family_id = get_family_id(token)
    return token, family_id


def test_privacy_export_happy():
    token, family_id = _family()
    _, body = api(
        "POST",
        "/privacy/export",
        {"familyId": family_id, "requestType": "export", "confirmationText": "EXPORT FAMILY DATA"},
        token=token,
        expect=202,
    )
    assert body["familyId"] == family_id
    assert body["requestType"] == "export"


def test_privacy_export_bad_request():
    token, family_id = _family()
    status, _ = api(
        "POST",
        "/privacy/export",
        {"familyId": family_id, "requestType": "export", "confirmationText": "wrong"},
        token=token,
    )
    assert status == 400, f"expected HTTP 400, got {status}"


def test_privacy_export_unauthenticated():
    token, family_id = _family()
    status, _ = api(
        "POST",
        "/privacy/export",
        {"familyId": family_id, "requestType": "export", "confirmationText": "EXPORT FAMILY DATA"},
    )
    assert status == 401, f"expected HTTP 401, got {status}"


def test_privacy_delete_happy():
    token, family_id = _family()
    _, body = api(
        "POST",
        "/privacy/delete",
        {"familyId": family_id, "requestType": "delete", "confirmationText": "DELETE FAMILY DATA", "reason": "api test"},
        token=token,
        expect=202,
    )
    assert body["familyId"] == family_id
    assert body["requestType"] == "delete"


def test_privacy_delete_bad_request():
    token, family_id = _family()
    status, _ = api(
        "POST",
        "/privacy/delete",
        {"familyId": family_id, "requestType": "delete", "confirmationText": "wrong"},
        token=token,
    )
    assert status == 400, f"expected HTTP 400, got {status}"


def test_privacy_delete_unauthenticated():
    token, family_id = _family()
    status, _ = api(
        "POST",
        "/privacy/delete",
        {"familyId": family_id, "requestType": "delete", "confirmationText": "DELETE FAMILY DATA"},
    )
    assert status == 401, f"expected HTTP 401, got {status}"


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
