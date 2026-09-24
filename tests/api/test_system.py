import sys
sys.path.insert(0, 'tests/api')
from conftest import api, get_auth_token, get_family_id, get_child_id


def test_internal_version_happy():
    _, body = api("GET", "/internal/version", expect=200)
    assert body["service"] == "core-api"
    assert body["status"] == "ok"


def test_internal_version_ignores_authentication():
    token = get_auth_token()
    _, body = api("GET", "/internal/version", token=token, expect=200)
    assert body["service"] == "core-api"


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
