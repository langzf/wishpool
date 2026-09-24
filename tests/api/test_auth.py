import sys
sys.path.insert(0, "tests/api")
from conftest import api, get_auth_token, get_family_id, get_child_id

import uuid


def _phone_code(phone_number=None):
    phone_number = phone_number or f"+1555{uuid.uuid4().int % 10_000_000:07d}"
    _, body = api(
        "POST",
        "/auth/phone-codes",
        {"phoneNumber": phone_number, "purpose": "login"},
        expect=200,
    )
    token = body.get("verificationToken")
    assert token, f"verificationToken missing: {body!r}"
    return token


def _login_pair():
    verification_token = _phone_code()
    _, body = api(
        "POST",
        "/auth/login",
        {
            "provider": "phone",
            "credential": f"{verification_token}:123456",
            "device": {"platform": "web", "deviceName": "api-test"},
        },
        expect=200,
    )
    assert body.get("accessToken"), f"accessToken missing: {body!r}"
    assert body.get("refreshToken"), f"refreshToken missing: {body!r}"
    return body


def test_phone_codes_happy_path():
    token = _phone_code()
    assert isinstance(token, str) and token


def test_phone_codes_missing_params_400():
    api("POST", "/auth/phone-codes", {"phoneNumber": "+15551234567"}, expect=400)


def test_phone_codes_invalid_params_400():
    api(
        "POST",
        "/auth/phone-codes",
        {"phoneNumber": "+15551234567", "purpose": "signup"},
        expect=400,
    )


def test_login_happy_path():
    body = _login_pair()
    assert body["user"]["id"], f"user id missing: {body!r}"


def test_login_missing_params_400():
    api("POST", "/auth/login", {"provider": "phone"}, expect=400)


def test_login_invalid_params_400():
    api(
        "POST",
        "/auth/login",
        {"provider": "email", "credential": "not-supported"},
        expect=400,
    )


def test_login_invalid_code_401():
    verification_token = _phone_code()
    api(
        "POST",
        "/auth/login",
        {"provider": "phone", "credential": f"{verification_token}:000000"},
        expect=401,
    )


def test_refresh_happy_path():
    pair = _login_pair()
    _, body = api("POST", "/auth/refresh", {"refreshToken": pair["refreshToken"]}, expect=200)
    assert body.get("accessToken"), f"accessToken missing: {body!r}"
    assert body.get("refreshToken"), f"refreshToken missing: {body!r}"


def test_refresh_missing_params_400():
    api("POST", "/auth/refresh", {}, expect=400)


def test_refresh_invalid_token_401():
    api("POST", "/auth/refresh", {"refreshToken": "not-a-refresh-token"}, expect=401)


def test_me_happy_path():
    token = get_auth_token()
    _, body = api("GET", "/me", token=token, expect=200)
    assert body["user"]["id"], f"user id missing: {body!r}"
    assert isinstance(body.get("families"), list), f"families must be a list: {body!r}"


def test_me_unauthenticated_401():
    api("GET", "/me", expect=401)


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
