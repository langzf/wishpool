class WishPoolRuntimeConfig {
  const WishPoolRuntimeConfig({
    required this.coreApiBaseUrl,
    required this.realtimeBaseUrl,
    required this.useRemoteApi,
    required this.accessToken,
    required this.familyId,
    required this.childId,
    required this.role,
  });

  final String coreApiBaseUrl;
  final String realtimeBaseUrl;
  final bool useRemoteApi;
  final String accessToken;
  final String familyId;
  final String childId;
  final String role;

  static const local = WishPoolRuntimeConfig(
    coreApiBaseUrl: String.fromEnvironment('WISHPOOL_CORE_API_URL', defaultValue: 'http://localhost:8080'),
    realtimeBaseUrl: String.fromEnvironment('WISHPOOL_REALTIME_URL', defaultValue: 'ws://localhost:8081/realtime'),
    useRemoteApi: bool.fromEnvironment('WISHPOOL_USE_REMOTE_API'),
    accessToken: String.fromEnvironment('WISHPOOL_ACCESS_TOKEN'),
    familyId: String.fromEnvironment('WISHPOOL_FAMILY_ID'),
    childId: String.fromEnvironment('WISHPOOL_CHILD_ID'),
    role: String.fromEnvironment('WISHPOOL_ROLE', defaultValue: 'parent'),
  );

  bool get hasRemoteContext => useRemoteApi && accessToken.isNotEmpty && familyId.isNotEmpty && childId.isNotEmpty;

  bool get isChildDevice => role == 'child_device';

  WishPoolRuntimeConfig copyWith({
    String? coreApiBaseUrl,
    String? realtimeBaseUrl,
    bool? useRemoteApi,
    String? accessToken,
    String? familyId,
    String? childId,
    String? role,
  }) {
    return WishPoolRuntimeConfig(
      coreApiBaseUrl: coreApiBaseUrl ?? this.coreApiBaseUrl,
      realtimeBaseUrl: realtimeBaseUrl ?? this.realtimeBaseUrl,
      useRemoteApi: useRemoteApi ?? this.useRemoteApi,
      accessToken: accessToken ?? this.accessToken,
      familyId: familyId ?? this.familyId,
      childId: childId ?? this.childId,
      role: role ?? this.role,
    );
  }
}
