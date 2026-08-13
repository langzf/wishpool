class WishPoolSession {
  const WishPoolSession({
    required this.accessToken,
    required this.refreshToken,
    required this.familyId,
    required this.childId,
    required this.role,
    required this.displayName,
  });

  final String accessToken;
  final String refreshToken;
  final String familyId;
  final String childId;
  final String role;
  final String displayName;

  bool get hasFamily => familyId.isNotEmpty;

  bool get hasChild => childId.isNotEmpty;

  bool get isChildDevice => role == 'child_device';

  WishPoolSession copyWith({
    String? accessToken,
    String? refreshToken,
    String? familyId,
    String? childId,
    String? role,
    String? displayName,
  }) {
    return WishPoolSession(
      accessToken: accessToken ?? this.accessToken,
      refreshToken: refreshToken ?? this.refreshToken,
      familyId: familyId ?? this.familyId,
      childId: childId ?? this.childId,
      role: role ?? this.role,
      displayName: displayName ?? this.displayName,
    );
  }
}
