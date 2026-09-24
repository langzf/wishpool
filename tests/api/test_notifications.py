import sys
sys.path.insert(0, 'tests/api')
from conftest import api, get_auth_token, get_family_id, get_child_id


MISSING_UUID = "00000000-0000-0000-0000-000000000000"


def _family():
    token = get_auth_token()
    family_id = get_family_id(token)
    return token, family_id


def test_list_notifications_happy():
    token, family_id = _family()
    _, body = api("GET", f"/notifications?familyId={family_id}&limit=10", token=token, expect=200)
    assert isinstance(body.get("items"), list), f"items is not a list: {body!r}"


def test_list_notifications_bad_request():
    token, family_id = _family()
    status, _ = api("GET", f"/notifications?familyId={family_id}&status=bogus", token=token)
    assert status == 400, f"expected HTTP 400, got {status}"


def test_list_notifications_unauthenticated():
    token, family_id = _family()
    status, _ = api("GET", f"/notifications?familyId={family_id}")
    assert status == 401, f"expected HTTP 401, got {status}"


def test_mark_notifications_read_happy():
    token, _ = _family()
    _, body = api("POST", "/notifications/read", {"notificationIds": [MISSING_UUID]}, token=token, expect=200)
    assert isinstance(body.get("items"), list), f"items is not a list: {body!r}"


def test_mark_notifications_read_bad_request():
    token, _ = _family()
    status, _ = api("POST", "/notifications/read", {"notificationIds": []}, token=token)
    assert status == 400, f"expected HTTP 400, got {status}"


def test_mark_notifications_read_unauthenticated():
    status, _ = api("POST", "/notifications/read", {"notificationIds": [MISSING_UUID]})
    assert status == 401, f"expected HTTP 401, got {status}"


def test_notification_preferences_happy():
    token, family_id = _family()
    _, body = api("GET", f"/notification-preferences?familyId={family_id}", token=token, expect=200)
    assert isinstance(body, list), f"preferences response is not a list: {body!r}"


def test_notification_preferences_unauthenticated():
    token, family_id = _family()
    status, _ = api("GET", f"/notification-preferences?familyId={family_id}")
    assert status == 401, f"expected HTTP 401, got {status}"


def test_update_notification_preferences_happy():
    token, family_id = _family()
    _, body = api(
        "PUT",
        "/notification-preferences",
        {
            "familyId": family_id,
            "notificationType": "review_completed",
            "enabled": True,
            "quietHours": {"enabled": False},
            "channels": {"inbox": True, "push": False},
        },
        token=token,
        expect=200,
    )
    assert body["familyId"] == family_id
    assert body["notificationType"] == "review_completed"


def test_update_notification_preferences_bad_request():
    token, family_id = _family()
    status, _ = api(
        "PUT",
        "/notification-preferences",
        {"familyId": family_id, "notificationType": "bogus", "enabled": True},
        token=token,
    )
    assert status == 400, f"expected HTTP 400, got {status}"


def test_update_notification_preferences_unauthenticated():
    token, family_id = _family()
    status, _ = api(
        "PUT",
        "/notification-preferences",
        {"familyId": family_id, "notificationType": "review_completed", "enabled": True},
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
