import 'package:flutter/widgets.dart';

import '../domain/wishpool_snapshot.dart';
import '../infrastructure/runtime_config.dart';
import '../infrastructure/wishpool_api_client.dart';
import 'sync_coordinator.dart';
import 'upload_queue.dart';
import 'wishpool_repository.dart';

class WishPoolScope extends InheritedWidget {
  WishPoolScope({
    super.key,
    required super.child,
    WishPoolRuntimeConfig config = WishPoolRuntimeConfig.local,
  }) : this._(
          key: key,
          child: child,
          config: config,
          apiClient: WishPoolApiClient(config: config),
        );

  WishPoolScope._({
    super.key,
    required super.child,
    required this.config,
    required this.apiClient,
  })  : repository = WishPoolRepository(
          config: config,
          apiClient: apiClient,
        ),
        syncCoordinator = SyncCoordinator(config: config),
        uploadQueue = UploadQueue();

  final WishPoolRuntimeConfig config;
  final WishPoolApiClient apiClient;
  final WishPoolRepository repository;
  final SyncCoordinator syncCoordinator;
  final UploadQueue uploadQueue;

  static WishPoolScope of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<WishPoolScope>();
    assert(scope != null, 'WishPoolScope is required above this widget.');
    return scope!;
  }

  Future<WishPoolSnapshot> loadSnapshot() => repository.loadHomeSnapshot();

  @override
  bool updateShouldNotify(WishPoolScope oldWidget) => config != oldWidget.config;
}
