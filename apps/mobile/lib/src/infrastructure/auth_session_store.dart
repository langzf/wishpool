import 'package:shared_preferences/shared_preferences.dart';

import '../auth/session.dart';

class AuthSessionStore {
  const AuthSessionStore();

  static const _accessToken = 'wishpool.accessToken';
  static const _refreshToken = 'wishpool.refreshToken';
  static const _familyId = 'wishpool.familyId';
  static const _childId = 'wishpool.childId';
  static const _role = 'wishpool.role';
  static const _displayName = 'wishpool.displayName';

  Future<WishPoolSession?> load() async {
    final prefs = await SharedPreferences.getInstance();
    final accessToken = prefs.getString(_accessToken) ?? '';
    if (accessToken.isEmpty) return null;
    return WishPoolSession(
      accessToken: accessToken,
      refreshToken: prefs.getString(_refreshToken) ?? '',
      familyId: prefs.getString(_familyId) ?? '',
      childId: prefs.getString(_childId) ?? '',
      role: prefs.getString(_role) ?? '',
      displayName: prefs.getString(_displayName) ?? '',
    );
  }

  Future<void> save(WishPoolSession session) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_accessToken, session.accessToken);
    await prefs.setString(_refreshToken, session.refreshToken);
    await prefs.setString(_familyId, session.familyId);
    await prefs.setString(_childId, session.childId);
    await prefs.setString(_role, session.role);
    await prefs.setString(_displayName, session.displayName);
  }

  Future<void> clear() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_accessToken);
    await prefs.remove(_refreshToken);
    await prefs.remove(_familyId);
    await prefs.remove(_childId);
    await prefs.remove(_role);
    await prefs.remove(_displayName);
  }
}
