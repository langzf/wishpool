import sys
sys.path.insert(0, 'tests/api')
from conftest import api, get_auth_token, get_family_id, get_child_id

import urllib.error
import urllib.parse
import urllib.request


MISSING_UUID = "00000000-0000-0000-0000-000000000000"


def _upload_session(token=None):
    token = token or get_auth_token()
    family_id = get_family_id(token)
    _, body = api(
        "POST",
        "/media/upload-sessions",
        {
            "familyId": family_id,
            "purpose": "wish_image",
            "contentType": "image/png",
            "sizeBytes": 8,
        },
        token=token,
        expect=201,
    )
    assert body.get("mediaId"), f"upload session missing mediaId: {body!r}"
    assert body.get("uploadUrl"), f"upload session missing uploadUrl: {body!r}"
    return token, body


def _put_upload(url):
    headers = {"Content-Type": "image/png", "Content-Length": "8"}
    target_url = url
    parsed = urllib.parse.urlsplit(url)
    if parsed.hostname == "minio":
        port = f":{parsed.port}" if parsed.port else ""
        target_url = urllib.parse.urlunsplit(
            (parsed.scheme, f"localhost{port}", parsed.path, parsed.query, parsed.fragment)
        )
        headers["Host"] = parsed.netloc
    request = urllib.request.Request(
        target_url,
        data=b"wishpool",
        headers=headers,
        method="PUT",
    )
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            assert 200 <= response.status < 300, f"upload PUT got HTTP {response.status}"
    except urllib.error.HTTPError as error:
        raise AssertionError(f"upload PUT got HTTP {error.code}: {error.read()!r}") from error


def test_create_upload_session_happy():
    _, body = _upload_session()
    assert body["storageKey"]
    assert body["maxSizeBytes"] >= 8


def test_create_upload_session_bad_request():
    token = get_auth_token()
    family_id = get_family_id(token)
    status, _ = api(
        "POST",
        "/media/upload-sessions",
        {
            "familyId": family_id,
            "purpose": "wish_image",
            "contentType": "",
            "sizeBytes": 8,
        },
        token=token,
    )
    assert status == 400, f"expected HTTP 400, got {status}"


def test_create_upload_session_unauthenticated():
    token = get_auth_token()
    family_id = get_family_id(token)
    status, _ = api(
        "POST",
        "/media/upload-sessions",
        {
            "familyId": family_id,
            "purpose": "wish_image",
            "contentType": "image/png",
            "sizeBytes": 8,
        },
    )
    assert status == 401, f"expected HTTP 401, got {status}"


def test_finalize_media_happy():
    token, session = _upload_session()
    _put_upload(session["uploadUrl"])
    _, body = api(
        "POST",
        f"/media/{session['mediaId']}/finalize",
        {"checksumSha256": "a" * 64, "width": 1, "height": 1},
        token=token,
        expect=200,
    )
    assert body["id"] == session["mediaId"]
    assert body["status"] == "uploaded"


def test_finalize_media_bad_request():
    token, session = _upload_session()
    status, _ = api(
        "POST",
        f"/media/{session['mediaId']}/finalize",
        {"checksumSha256": "not-a-sha"},
        token=token,
    )
    assert status == 400, f"expected HTTP 400, got {status}"


def test_finalize_media_unauthenticated():
    token, session = _upload_session()
    status, _ = api(
        "POST",
        f"/media/{session['mediaId']}/finalize",
        {"checksumSha256": "a" * 64},
    )
    assert status == 401, f"expected HTTP 401, got {status}"


def test_finalize_media_not_found():
    token = get_auth_token()
    status, _ = api(
        "POST",
        f"/media/{MISSING_UUID}/finalize",
        {"checksumSha256": "a" * 64},
        token=token,
    )
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
