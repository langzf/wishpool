import sys
sys.path.insert(0, "tests/api")
from conftest import api, get_auth_token, get_family_id, get_child_id


MISSING_UUID = "00000000-0000-0000-0000-000000000000"


def _pairing_session():
    token = get_auth_token()
    family_id = get_family_id(token)
    child_id = get_child_id(token, family_id)
    _, body = api(
        "POST",
        f"/families/{family_id}/pairing-sessions",
        {"childId": child_id},
        token=token,
        expect=201,
    )
    assert body.get("pairingCode"), f"pairingCode missing: {body!r}"
    return body


def test_create_pairing_session_happy_path():
    body = _pairing_session()
    assert body.get("sessionId"), f"sessionId missing: {body!r}"


def test_create_pairing_session_missing_params_400():
    token = get_auth_token()
    family_id = get_family_id(token)
    api("POST", f"/families/{family_id}/pairing-sessions", {}, token=token, expect=400)


def test_create_pairing_session_invalid_params_400():
    token = get_auth_token()
    api(
        "POST",
        "/families/not-a-uuid/pairing-sessions",
        {"childId": MISSING_UUID},
        token=token,
        expect=400,
    )


def test_create_pairing_session_unauthenticated_401():
    api("POST", f"/families/{MISSING_UUID}/pairing-sessions", {"childId": MISSING_UUID}, expect=401)


def test_create_pairing_session_child_not_found_404():
    token = get_auth_token()
    family_id = get_family_id(token)
    api(
        "POST",
        f"/families/{family_id}/pairing-sessions",
        {"childId": MISSING_UUID},
        token=token,
        expect=404,
    )


def test_consume_pairing_happy_path():
    session = _pairing_session()
    _, body = api(
        "POST",
        "/pairing/consume",
        {
            "pairingCode": session["pairingCode"],
            "device": {"platform": "web", "deviceName": "child-device-api-test"},
        },
        expect=200,
    )
    assert body.get("accessToken"), f"accessToken missing: {body!r}"
    assert body.get("refreshToken"), f"refreshToken missing: {body!r}"


def test_consume_pairing_missing_params_400():
    api("POST", "/pairing/consume", {"pairingCode": "123-456-789"}, expect=400)


def test_consume_pairing_invalid_code_401():
    api(
        "POST",
        "/pairing/consume",
        {"pairingCode": "000-000-000", "device": {"platform": "web"}},
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
