import 'dart:io';

import 'package:flutter/widgets.dart';

import '../domain/wishpool_snapshot.dart';
import '../infrastructure/runtime_config.dart';
import '../infrastructure/wishpool_api_client.dart';
import '../shared/fixture_data.dart';
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
          uploadQueue: UploadQueue(),
        );

  WishPoolScope._({
    super.key,
    required super.child,
    required this.config,
    required this.apiClient,
    required this.uploadQueue,
  })  : repository = WishPoolRepository(
          config: config,
          apiClient: apiClient,
          uploadQueue: uploadQueue,
        ),
        syncCoordinator = SyncCoordinator(config: config);

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

  Future<void> approveReview(String submissionId) => repository.approveReview(submissionId);

  Future<void> requestRevision(String submissionId) => repository.requestRevision(submissionId);

  Future<void> submitManualTask(ChildTask task) => repository.submitManualTask(task);

  Future<void> submitMediaTask({
    required ChildTask task,
    required File file,
    required String contentType,
  }) =>
      repository.submitMediaTask(task: task, file: file, contentType: contentType);

  Future<void> arrangeRoomItem(RoomItemData item, {required double left, required double top}) =>
      repository.arrangeRoomItem(item, left: left, top: top);

  Future<void> skipTask(ChildTask task) => repository.skipTask(task);

  Future<void> postponeTask(ChildTask task) => repository.postponeTask(task);

  Future<void> markNotificationsRead(List<String> notificationIds) => repository.markNotificationsRead(notificationIds);

  Future<void> updateNotificationPreference(NotificationPreferenceData preference, {required bool enabled}) =>
      repository.updateNotificationPreference(preference, enabled: enabled);

  @override
  bool updateShouldNotify(WishPoolScope oldWidget) => config != oldWidget.config;
}
