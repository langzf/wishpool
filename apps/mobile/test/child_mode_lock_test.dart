import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:wishpool_mobile/src/infrastructure/child_mode_lock.dart';

void main() {
  test('PIN is salted, persisted, and survives a new store instance', () async {
    SharedPreferences.setMockInitialValues({});
    final store = ChildModeLockStore();
    await store.setPin('1234');
    final prefs = await SharedPreferences.getInstance();
    expect(await store.isEnabled, isTrue);
    expect(prefs.getString(ChildModeLockStore.digestKey), isNot('1234'));
    expect(prefs.getString(ChildModeLockStore.saltKey), isNotNull);
    expect(prefs.getString(ChildModeLockStore.digestKey), isNotNull);
    expect((await ChildModeLockStore().verify('1234')).accepted, isTrue);
    expect((await ChildModeLockStore().verify('9999')).accepted, isFalse);
  });

  test('five wrong PINs start a persisted 30 second cooldown', () async {
    SharedPreferences.setMockInitialValues({});
    final store = ChildModeLockStore();
    await store.setPin('1234');
    for (var i = 0; i < 5; i++) {
      await store.verify('0000');
    }
    final attempt = await ChildModeLockStore().verify('1234');
    expect(attempt.accepted, isFalse);
    expect(attempt.coolingDown, isTrue);
  });
}
