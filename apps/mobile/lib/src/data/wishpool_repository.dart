import '../domain/wishpool_snapshot.dart';
import '../infrastructure/runtime_config.dart';
import '../infrastructure/wishpool_api_client.dart';

class WishPoolRepository {
  const WishPoolRepository({
    required this.config,
    required this.apiClient,
  });

  final WishPoolRuntimeConfig config;
  final WishPoolApiClient apiClient;

  Future<WishPoolSnapshot> loadHomeSnapshot() async {
    if (!config.useRemoteApi) return fixtureSnapshot;
    try {
      await apiClient.getJson('/actuator/health');
      return const WishPoolSnapshot(
        source: 'core-api',
        childName: fixtureSnapshot.childName,
        wishTitle: fixtureSnapshot.wishTitle,
        wishProgress: fixtureSnapshot.wishProgress,
        childTasks: fixtureSnapshot.childTasks,
        reviewCards: fixtureSnapshot.reviewCards,
      );
    } catch (_) {
      return fixtureSnapshot;
    }
  }
}
