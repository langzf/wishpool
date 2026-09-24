import json
import random
import urllib.error
import urllib.request


BASE_URL = "http://localhost:18080"
_AUTH_TOKEN = None
_FAMILY_ID = None
_CHILD_ID = None


def api(method, path, payload=None, token=None, expect=None, headers=None):
    url = BASE_URL + path
    data = None
    request_headers = {"Accept": "application/json"}

    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
        request_headers["Content-Type"] = "application/json"

    if token is not None:
        request_headers["Authorization"] = "Bearer " + token

    if headers:
        request_headers.update(headers)

    request = urllib.request.Request(url, data=data, headers=request_headers, method=method)

    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            status = response.status
            body = response.read().decode("utf-8")
    except urllib.error.HTTPError as error:
        status = error.code
        body = error.read().decode("utf-8")
    except urllib.error.URLError as error:
        raise AssertionError(
            f"{method} {path} failed to connect to {url}: {error}"
        ) from error

    parsed_body = None
    if body:
        try:
            parsed_body = json.loads(body)
        except json.JSONDecodeError:
            parsed_body = body

    if expect is not None:
        assert status == expect, (
            f"{method} {path} expected HTTP {expect}, got HTTP {status}. "
            f"Response body: {parsed_body!r}"
        )

    return status, parsed_body


def get_auth_token():
    global _AUTH_TOKEN
    if _AUTH_TOKEN is not None:
        return _AUTH_TOKEN

    phone_number = "1" + "".join(str(random.randrange(10)) for _ in range(10))
    _, code_body = api(
        "POST",
        "/auth/phone-codes",
        {"phoneNumber": phone_number, "purpose": "login"},
        expect=200,
    )
    verification_token = code_body.get("verificationToken")
    assert verification_token, (
        "POST /auth/phone-codes did not return verificationToken: "
        f"{code_body!r}"
    )

    _, login_body = api(
        "POST",
        "/auth/login",
        {
            "provider": "phone",
            "credential": f"{verification_token}:123456",
            "device": {"platform": "web", "deviceName": "test"},
        },
        expect=200,
    )
    access_token = login_body.get("accessToken")
    assert access_token, f"POST /auth/login did not return accessToken: {login_body!r}"
    _AUTH_TOKEN = access_token
    return _AUTH_TOKEN


def get_family_id(token=None):
    global _FAMILY_ID
    if token is None:
        token = get_auth_token()
    if token == _AUTH_TOKEN and _FAMILY_ID is not None:
        return _FAMILY_ID
    _, family_body = api(
        "POST",
        "/families",
        {
            "name": "API Test Family " + "".join(str(random.randrange(10)) for _ in range(8)),
            "timezone": "Asia/Shanghai",
        },
        token=token,
        expect=201,
    )
    created_id = family_body.get("id")
    assert created_id, f"POST /families did not return id: {family_body!r}"
    if token == _AUTH_TOKEN:
        _FAMILY_ID = created_id
    return created_id


def get_child_id(token=None, family_id=None):
    global _CHILD_ID
    if token is None:
        token = get_auth_token()
    if token == _AUTH_TOKEN and family_id is None and _CHILD_ID is not None:
        return _CHILD_ID
    if family_id is None:
        family_id = get_family_id(token)
    _, child_body = api(
        "POST",
        f"/families/{family_id}/children",
        {
            "nickname": "API Test Child " + "".join(str(random.randrange(10)) for _ in range(6)),
            "birthYear": 2016,
            "roomTheme": "forest",
        },
        token=token,
        expect=201,
    )
    created_id = child_body.get("id")
    assert created_id, (
        f"POST /families/{family_id}/children did not return id: {child_body!r}"
    )
    if token == _AUTH_TOKEN and family_id == _FAMILY_ID:
        _CHILD_ID = created_id
    return created_id
