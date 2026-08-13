import 'dart:io';

import '../auth/session.dart';
import '../infrastructure/runtime_config.dart';
import '../infrastructure/wishpool_api_client.dart';

class PhoneCodeResult {
  const PhoneCodeResult({
    required this.verificationToken,
    required this.debugCode,
  });

  final String verificationToken;
  final String? debugCode;
}

class AuthRepository {
  const AuthRepository({
    required this.config,
    required this.apiClient,
  });

  final WishPoolRuntimeConfig config;
  final WishPoolApiClient apiClient;

  Future<PhoneCodeResult> requestPhoneCode(String phoneNumber) async {
    final response = await apiClient.postJson(
      '/auth/phone-codes',
      <String, Object?>{
        'phoneNumber': phoneNumber,
        'purpose': 'login',
      },
    );
    return PhoneCodeResult(
      verificationToken: _string(response['verificationToken'], ''),
      debugCode: _stringOrNull(response['debugCode']),
    );
  }

  Future<WishPoolSession> loginWithPhone({
    required String verificationToken,
    required String code,
  }) async {
    final response = await apiClient.postJson(
      '/auth/login',
      <String, Object?>{
        'provider': 'phone',
        'credential': '$verificationToken:$code',
        'device': _deviceRegistration('家长手机'),
      },
    );
    return _sessionFromAuthResponse(response);
  }

  Future<WishPoolSession> pairChildDevice(String pairingCode) async {
    final response = await apiClient.postJson(
      '/pairing/consume',
      <String, Object?>{
        'pairingCode': pairingCode,
        'device': _deviceRegistration('儿童设备'),
      },
    );
    return _sessionFromAuthResponse(response);
  }

  Future<WishPoolSession> refresh(WishPoolSession session) async {
    if (session.refreshToken.isEmpty) return enrichSession(session);
    final response = await apiClient.postJson(
      '/auth/refresh',
      <String, Object?>{'refreshToken': session.refreshToken},
    );
    return _sessionFromAuthResponse(response);
  }

  Future<WishPoolSession> createFamilyWithChild({
    required WishPoolSession session,
    required String familyName,
    required String childName,
    required String timezone,
  }) async {
    final family = await apiClient.postJson(
      '/families',
      <String, Object?>{
        'name': familyName,
        'timezone': timezone,
        'firstChild': <String, Object?>{
          'nickname': childName,
          'roomTheme': 'forest',
        },
      },
      accessToken: session.accessToken,
      idempotencyKey: 'mobile-create-family-${DateTime.now().millisecondsSinceEpoch}',
    );
    final familyId = _string(family['id'], '');
    final children = familyId.isEmpty ? const <Object?>[] : await _children(familyId, session.accessToken);
    final childId = _firstId(children);
    return session.copyWith(
      familyId: familyId,
      childId: childId,
      role: session.role.isEmpty ? 'parent_owner' : session.role,
    );
  }

  Future<WishPoolSession> enrichSession(WishPoolSession session) async {
    if (session.accessToken.isEmpty) return session;
    try {
      final context = await apiClient.getJson('/me', accessToken: session.accessToken);
      final families = context['families'] is List ? context['families'] as List : const [];
      final current = _preferredFamilyContext(families, session.familyId);
      final family = _map(current['family']);
      final member = _map(current['member']);
      final familyId = _string(family['id'], session.familyId);
      final role = _string(member['role'], session.role);
      final directChildId = _string(member['childId'], session.childId);
      final childId = directChildId.isNotEmpty || familyId.isEmpty
          ? directChildId
          : _firstId(await _children(familyId, session.accessToken));
      return session.copyWith(
        familyId: familyId,
        childId: childId,
        role: role,
      );
    } catch (_) {
      return session;
    }
  }

  Future<WishPoolSession> _sessionFromAuthResponse(Map<String, Object?> response) async {
    final user = _map(response['user']);
    final session = WishPoolSession(
      accessToken: _string(response['accessToken'], ''),
      refreshToken: _string(response['refreshToken'], ''),
      familyId: _string(response['primaryFamilyId'], ''),
      childId: '',
      role: '',
      displayName: _string(user['displayName'], 'WishPool'),
    );
    return enrichSession(session);
  }

  Future<List<Object?>> _children(String familyId, String accessToken) async {
    final response = await apiClient.getRawJson('/families/$familyId/children', accessToken: accessToken);
    return response is List ? response : const [];
  }

  Map<Object?, Object?> _preferredFamilyContext(List<Object?> families, String familyId) {
    if (families.isEmpty) return const {};
    final exact = families.map(_map).where((context) => _string(_map(context['family'])['id'], '') == familyId);
    return exact.isNotEmpty ? exact.first : _map(families.first);
  }

  Map<String, Object?> _deviceRegistration(String deviceName) {
    return <String, Object?>{
      'platform': _platform(),
      'deviceName': deviceName,
    };
  }

  String _platform() {
    if (Platform.isIOS) return 'ios';
    if (Platform.isAndroid) return 'android';
    if (Platform.isMacOS) return 'ipad_os';
    return 'web';
  }

  String _firstId(List<Object?> values) {
    if (values.isEmpty) return '';
    return _string(_map(values.first)['id'], '');
  }

  Map<Object?, Object?> _map(Object? value) => value is Map ? value : const {};

  String _string(Object? value, String fallback) => value is String && value.isNotEmpty ? value : fallback;

  String? _stringOrNull(Object? value) => value is String && value.isNotEmpty ? value : null;
}
