class WishPoolRuntimeConfig {
  const WishPoolRuntimeConfig({
    required this.coreApiBaseUrl,
    required this.realtimeBaseUrl,
    required this.useRemoteApi,
  });

  final String coreApiBaseUrl;
  final String realtimeBaseUrl;
  final bool useRemoteApi;

  static const local = WishPoolRuntimeConfig(
    coreApiBaseUrl: String.fromEnvironment('WISHPOOL_CORE_API_URL', defaultValue: 'http://localhost:8080'),
    realtimeBaseUrl: String.fromEnvironment('WISHPOOL_REALTIME_URL', defaultValue: 'ws://localhost:8081/realtime'),
    useRemoteApi: bool.fromEnvironment('WISHPOOL_USE_REMOTE_API'),
  );
}
