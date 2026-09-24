import sys
sys.path.insert(0, 'tests/api')
from conftest import api, get_auth_token, get_family_id, get_child_id

import json
import os
import urllib.error
import urllib.request


BASE_URL = "http://localhost:18080"


def _env_value(key):
    if os.environ.get(key):
        return os.environ[key]
    try:
        with open(".env", "r", encoding="utf-8") as env_file:
            for line in env_file:
                stripped = line.strip()
                if not stripped or stripped.startswith("#") or "=" not in stripped:
                    continue
                name, value = stripped.split("=", 1)
                if name.strip() == key:
                    return value.strip().strip('"').strip("'")
    except FileNotFoundError:
        pass
    return None


def _admin_token():
    return _env_value("WISHPOOL_ADMIN_TOKEN") or _env_value("WISHPOOL_INTERNAL_TOKEN") or "wishpool-local-internal-token"


def _admin_api(method, path, token=None):
    headers = {"Accept": "application/json"}
    if token is not None:
        headers["X-Internal-Token"] = token
    request = urllib.request.Request(BASE_URL + path, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            status = response.status
            body = response.read().decode("utf-8")
    except urllib.error.HTTPError as error:
        status = error.code
        body = error.read().decode("utf-8")
    parsed = None
    if body:
        try:
            parsed = json.loads(body)
        except json.JSONDecodeError:
            parsed = body
    return status, parsed


def test_admin_dashboard_happy():
    status, body = _admin_api("GET", "/internal/admin/dashboard", _admin_token())
    assert status == 200, f"expected HTTP 200, got {status}: {body!r}"
    assert body["familyCount"] >= 0
    assert body["generatedAt"]


def test_admin_dashboard_unauthenticated():
    status, _ = _admin_api("GET", "/internal/admin/dashboard")
    assert status == 401, f"expected HTTP 401, got {status}"


def test_admin_families_happy():
    status, body = _admin_api("GET", "/internal/admin/families?limit=5", _admin_token())
    assert status == 200, f"expected HTTP 200, got {status}: {body!r}"
    assert isinstance(body, list), f"families response is not a list: {body!r}"


def test_admin_families_bad_request():
    status, _ = _admin_api("GET", "/internal/admin/families?status=bogus", _admin_token())
    assert status == 400, f"expected HTTP 400, got {status}"


def test_admin_families_unauthenticated():
    status, _ = _admin_api("GET", "/internal/admin/families")
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
