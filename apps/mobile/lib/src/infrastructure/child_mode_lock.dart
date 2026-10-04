// ignore_for_file: curly_braces_in_flow_control_structures
import 'dart:convert';
import 'dart:math';
import 'dart:typed_data';

import 'package:crypto/crypto.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class PinAttemptResult {
  const PinAttemptResult({required this.accepted, this.cooldownUntil});

  final bool accepted;
  final DateTime? cooldownUntil;

  bool get coolingDown =>
      cooldownUntil != null && cooldownUntil!.isAfter(DateTime.now());
}

/// Local-only child-mode lock. Only the salted digest and throttling metadata
/// are persisted; the PIN itself is never written to preferences.
class ChildModeLockStore {
  ChildModeLockStore({SharedPreferences? preferences})
      : _preferences = preferences;

  static const saltKey = 'wishpool.childModeLock.salt';
  static const digestKey = 'wishpool.childModeLock.digest';
  static const failuresKey = 'wishpool.childModeLock.failures';
  static const cooldownKey = 'wishpool.childModeLock.cooldownUntil';
  static const _iterations = 12000;
  final SharedPreferences? _preferences;

  Future<SharedPreferences> get _prefs async =>
      _preferences ?? await SharedPreferences.getInstance();

  Future<bool> get isEnabled async =>
      (await _prefs).getString(digestKey)?.isNotEmpty == true;

  Future<void> setPin(String pin) async {
    if (!RegExp(r'^\d{4,6}$').hasMatch(pin)) {
      throw const FormatException('PIN must contain 4 to 6 digits.');
    }
    final salt = List<int>.generate(16, (_) => Random.secure().nextInt(256));
    final prefs = await _prefs;
    await prefs.setString(saltKey, base64UrlEncode(salt));
    await prefs.setString(digestKey, _digest(pin, salt));
    await _clearThrottle(prefs);
  }

  Future<void> disable() async {
    final prefs = await _prefs;
    await prefs.remove(saltKey);
    await prefs.remove(digestKey);
    await _clearThrottle(prefs);
  }

  Future<PinAttemptResult> verify(String pin) async {
    final prefs = await _prefs;
    final cooldownMs = prefs.getInt(cooldownKey) ?? 0;
    final now = DateTime.now();
    if (cooldownMs > now.millisecondsSinceEpoch) {
      return PinAttemptResult(
          cooldownUntil: DateTime.fromMillisecondsSinceEpoch(cooldownMs),
          accepted: false);
    }
    final saltText = prefs.getString(saltKey);
    final expected = prefs.getString(digestKey);
    if (saltText == null || expected == null) {
      return const PinAttemptResult(accepted: false);
    }
    final accepted =
        _constantTimeEquals(expected, _digest(pin, base64Url.decode(saltText)));
    if (accepted) {
      await _clearThrottle(prefs);
      return const PinAttemptResult(accepted: true);
    }
    final failures = (prefs.getInt(failuresKey) ?? 0) + 1;
    if (failures >= 5) {
      final until = now.add(const Duration(seconds: 30));
      await prefs.setInt(failuresKey, 0);
      await prefs.setInt(cooldownKey, until.millisecondsSinceEpoch);
      return PinAttemptResult(accepted: false, cooldownUntil: until);
    }
    await prefs.setInt(failuresKey, failures);
    return const PinAttemptResult(accepted: false);
  }

  String _digest(String pin, List<int> salt) {
    var bytes = Uint8List.fromList([...salt, ...utf8.encode(pin)]);
    for (var i = 0; i < _iterations; i++) {
      bytes = Uint8List.fromList(sha256.convert(bytes).bytes);
    }
    return base64UrlEncode(bytes);
  }

  bool _constantTimeEquals(String a, String b) {
    if (a.length != b.length) return false;
    var result = 0;
    for (var i = 0; i < a.length; i++) {
      result |= a.codeUnitAt(i) ^ b.codeUnitAt(i);
    }
    return result == 0;
  }

  Future<void> _clearThrottle(SharedPreferences prefs) async {
    await prefs.remove(failuresKey);
    await prefs.remove(cooldownKey);
  }
}

String pinCounterText(int currentLength, int? maxLength) {
  if (maxLength == null) return '已输入 $currentLength 位';
  final remaining = maxLength - currentLength;
  return remaining > 0 ? '还可输入 $remaining 位' : '已输入 $currentLength 位';
}

Future<bool> requestParentPin(
    BuildContext context, ChildModeLockStore store) async {
  final controller = TextEditingController();
  var error = '';
  final result = await showDialog<bool>(
    context: context,
    barrierDismissible: false,
    builder: (dialogContext) => StatefulBuilder(
      builder: (context, setState) => AlertDialog(
        title: const Text('请输入家长密码'),
        content: Column(mainAxisSize: MainAxisSize.min, children: [
          TextField(
            controller: controller,
            autofocus: true,
            obscureText: true,
            keyboardType: TextInputType.number,
            maxLength: 6,
            decoration: InputDecoration(
                errorText: error.isEmpty ? null : error, hintText: '4–6 位数字'),
            buildCounter: (context,
                    {required currentLength, required isFocused, maxLength}) =>
                Text(pinCounterText(currentLength, maxLength)),
          ),
        ]),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('取消')),
          FilledButton(
            onPressed: () async {
              final attempt = await store.verify(controller.text.trim());
              if (attempt.accepted) {
                if (dialogContext.mounted) {
                  Navigator.pop(dialogContext, true);
                }
              } else {
                setState(() {
                  error = attempt.coolingDown
                      ? '尝试次数过多，请 ${attempt.cooldownUntil!.difference(DateTime.now()).inSeconds.clamp(1, 30)} 秒后再试'
                      : '密码不对，再试试';
                });
              }
            },
            child: const Text('确认'),
          ),
        ],
      ),
    ),
  );
  // Let the dialog route complete its removal before callers change the
  // authenticated widget subtree that opened it.
  await WidgetsBinding.instance.endOfFrame;
  controller.dispose();
  return result == true;
}

Future<void> showChildModeLockSettings(
    BuildContext context, ChildModeLockStore store) async {
  final enabled = await store.isEnabled;
  if (!context.mounted) return;
  final action = await showModalBottomSheet<String>(
    context: context,
    builder: (context) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
      ListTile(
          title: Text(enabled ? '修改儿童模式锁' : '设置儿童模式锁'),
          leading: const Icon(Icons.lock_outline),
          onTap: () => Navigator.pop(context, 'set')),
      if (enabled)
        ListTile(
            title: const Text('关闭儿童模式锁'),
            leading: const Icon(Icons.lock_open_outlined),
            onTap: () => Navigator.pop(context, 'disable')),
    ])),
  );
  if (!context.mounted || action == null) return;
  if (enabled && !await requestParentPin(context, store)) return;
  if (!context.mounted) return;
  if (action == 'disable') {
    await store.disable();
    if (!context.mounted) return;
    ScaffoldMessenger.of(context)
        .showSnackBar(const SnackBar(content: Text('儿童模式锁已关闭')));
    return;
  }
  final first = TextEditingController();
  final second = TextEditingController();
  final ok = await showDialog<bool>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      title: Text(enabled ? '修改儿童模式锁' : '设置儿童模式锁'),
      content: Column(mainAxisSize: MainAxisSize.min, children: [
        TextField(
            controller: first,
            autofocus: true,
            obscureText: true,
            keyboardType: TextInputType.number,
            maxLength: 6,
            decoration: const InputDecoration(labelText: '输入 4–6 位数字 PIN'),
            buildCounter: (context,
                    {required currentLength, required isFocused, maxLength}) =>
                Text(pinCounterText(currentLength, maxLength))),
        TextField(
            controller: second,
            obscureText: true,
            keyboardType: TextInputType.number,
            maxLength: 6,
            decoration: const InputDecoration(labelText: '再次输入 PIN'),
            buildCounter: (context,
                    {required currentLength, required isFocused, maxLength}) =>
                Text(pinCounterText(currentLength, maxLength))),
      ]),
      actions: [
        TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('取消')),
        FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('保存'))
      ],
    ),
  );
  if (ok != true) return;
  if (first.text != second.text || !RegExp(r'^\d{4,6}$').hasMatch(first.text)) {
    if (context.mounted)
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('请输入两次一致的 4–6 位数字 PIN')));
    return;
  }
  await store.setPin(first.text);
  if (context.mounted)
    ScaffoldMessenger.of(context)
        .showSnackBar(const SnackBar(content: Text('儿童模式锁已保存')));
}
