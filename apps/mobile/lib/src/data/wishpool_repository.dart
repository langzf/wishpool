import 'dart:io';
// ignore_for_file: curly_braces_in_flow_control_structures

import 'dart:async';


import '../domain/wishpool_snapshot.dart';
import '../infrastructure/runtime_config.dart';
import '../infrastructure/wishpool_api_client.dart';
import '../shared/fixture_data.dart';
import 'upload_queue.dart';
import '../media/media_capture_utils.dart';

enum MediaSubmitOutcome {
  completed,
  queued,
  failed,
  alreadySubmitted,
  inProgress
}

class WishPoolRepository {
  WishPoolRepository({
    required this.config,
    required this.apiClient,
    required this.uploadQueue,
  });

  final WishPoolRuntimeConfig config;
  final WishPoolApiClient apiClient;
  final UploadQueue uploadQueue;

  final Set<String> _processing = <String>{};

  Future<WishPoolSnapshot> loadHomeSnapshot() async {
    if (!config.hasRemoteContext) return fixtureSnapshot;
    try {
      final childHome = await apiClient.getJson(
        '/children/${config.childId}/home-context',
        accessToken: config.accessToken,
      );
      final parentHome = config.isChildDevice
          ? const <String, Object?>{}
          : await _parentHome();
      final notifications = await _notificationInbox();
      final preferences = await _notificationPreferences();
      final wishHistory = await _wishHistory();
      final memoryTimeline = await _memoryTimeline();
      final currentWish = _map(childHome['currentWish']);
      final fragmentVisual = _map(currentWish['fragmentVisual']);
      final latestFeedback = _feedback(childHome['latestFeedback']);
      return WishPoolSnapshot(
        source: 'core-api',
        childName: _string(
            _map(childHome['child'])['nickname'], fixtureSnapshot.childName),
        familyName: _string(_map(parentHome['family'])['name'], ''),
        wishTitle: _string(currentWish['title'], '还没有本周心愿'),
        wishProgress: _wishProgress(currentWish, fallbackOnEmpty: false),
        wishCurrentFragments: _int(currentWish['earnedFragments'], 0),
        wishTargetFragments: _positiveInt(currentWish['requiredFragments'], 1),
        wishImageUrl:
            _stringOrNull(_map(currentWish['imageMedia'])['downloadUrl']),
        wishFragmentVisualMode: _string(fragmentVisual['mode'], 'grid_reveal'),
        wishFragmentRows: _nullablePositiveInt(fragmentVisual['rows']),
        wishFragmentCols: _nullablePositiveInt(fragmentVisual['cols']),
        wishFragmentMask: _fragmentMask(fragmentVisual['mask']),
        wishLitIndexes: _intList(fragmentVisual['litIndexes']),
        childTasks: _tasks(_map(childHome['today'])['tasks'],
            fallbackOnEmpty: false, latestFeedback: latestFeedback),
        reviewCards: config.isChildDevice
            ? const <ReviewCardData>[]
            : _reviews(parentHome['pendingReviews'], fallbackOnEmpty: false),
        weeklyPlanRules: config.isChildDevice
            ? const <WeeklyPlanRuleData>[]
            : _planRules(_map(parentHome['weeklyPlan'])['rules'],
                fallbackOnEmpty: false),
        roomItems: _roomItems(_map(childHome['room'])['items'],
            fallbackOnEmpty: false),
        latestMemoryTitle:
            _stringOrNull(_map(childHome['latestMemory'])['title']),
        unreadNotifications: _int(childHome['unreadNotifications'], 0),
        notificationInbox:
            _notifications(notifications['items'], fallbackOnEmpty: false),
        notificationPreferences: _preferences(preferences),
        latestFeedback: latestFeedback,
        wishHistory: wishHistory,
        memoryTimeline: memoryTimeline,
      );
    } catch (_) {
      return fixtureSnapshot;
    }
  }

  Future<Map<String, Object?>> _parentHome() async {
    try {
      return await apiClient.getJson(
        '/families/${config.familyId}/parent-dashboard?childId=${config.childId}',
        accessToken: config.accessToken,
      );
    } catch (_) {
      return const <String, Object?>{};
    }
  }

  Future<Map<String, Object?>> _notificationInbox() async {
    try {
      return await apiClient.getJson(
        '/notifications?familyId=${config.familyId}&limit=20',
        accessToken: config.accessToken,
      );
    } catch (_) {
      return const <String, Object?>{};
    }
  }

  Future<Object?> _notificationPreferences() async {
    try {
      return await apiClient.getRawJson(
        '/notification-preferences?familyId=${config.familyId}',
        accessToken: config.accessToken,
      );
    } catch (_) {
      return const <Object?>[];
    }
  }

  Future<List<WishHistoryItemData>> _wishHistory() async {
    try {
      final response = await apiClient.getRawJson(
        '/children/${config.childId}/wishes/history',
        accessToken: config.accessToken,
      );
      final raw = response is Map && response['items'] is List
          ? response['items']
          : response;
      if (raw is! List) return const <WishHistoryItemData>[];
      return raw
          .whereType<Map>()
          .map((item) {
            final wish = _map(item['wish']);
            final redemption = _map(item['redemption']);
            final photos = redemption['photos'] is List
                ? (redemption['photos'] as List)
                    .whereType<Map>()
                    .map((photo) => _stringOrNull(photo['downloadUrl']))
                    .whereType<String>()
                    .toList()
                : const <String>[];
            return WishHistoryItemData(
              id: _string(wish['id'], ''),
              title: _string(wish['title'], '未命名心愿'),
              status: _string(wish['status'], 'unknown'),
              earnedFragments: _int(wish['earnedFragments'], 0),
              requiredFragments: _positiveInt(wish['requiredFragments'], 1),
              imageUrl: _stringOrNull(_map(wish['imageMedia'])['downloadUrl']),
              redeemedDate: _stringOrNull(redemption['redeemedDate']),
              redemptionPhotoUrls: photos,
            );
          })
          .where((item) => item.id.isNotEmpty)
          .toList();
    } catch (_) {
      return const <WishHistoryItemData>[];
    }
  }

  Future<List<MemoryTimelineItemData>> _memoryTimeline() async {
    try {
      final response = await apiClient.getJson(
        '/memories?childId=${config.childId}',
        accessToken: config.accessToken,
      );
      final raw = response['items'];
      if (raw is! List) return const <MemoryTimelineItemData>[];
      return raw
          .whereType<Map>()
          .map(_memory)
          .where((item) => item.id.isNotEmpty)
          .toList();
    } catch (_) {
      return const <MemoryTimelineItemData>[];
    }
  }

  MemoryTimelineItemData _memory(Map<Object?, Object?> value) {
    final summary = _map(value['summary']);
    final rawItems = value['items'];
    final items = rawItems is List
        ? rawItems.whereType<Map>().map(_memoryItem).toList()
        : const <MemoryMediaItemData>[];
    return MemoryTimelineItemData(
      id: _string(value['id'], ''),
      weekId: _string(value['weekId'], ''),
      title: _string(value['title'], '本周回忆'),
      status: _string(value['status'], 'generated'),
      summary: _string(summary['summary'], '本周的成长都被好好记录下来了。'),
      items: items,
    );
  }

  MemoryMediaItemData _memoryItem(Map<Object?, Object?> value) {
    final media = _map(value['media']);
    final url = _stringOrNull(value['downloadUrl']) ??
        _stringOrNull(media['downloadUrl']) ??
        _stringOrNull(value['url']);
    final kind = _string(value['itemType'], _string(value['kind'],
        _string(value['mediaType'], _string(media['contentType'], 'approved_task'))));
    return MemoryMediaItemData(
      id: _string(value['id'], _string(media['id'], 'memory-item')),
      title: _string(value['title'], _string(value['name'], '成长记录')),
      kind: kind,
      sourceType: _stringOrNull(value['sourceType']),
      sourceId: _stringOrNull(value['sourceId']),
      mediaAssetId: _stringOrNull(value['mediaAssetId']),
      contentType: _stringOrNull(value['contentType']) ?? _stringOrNull(media['contentType']),
      url: url,
      thumbnailUrl: _stringOrNull(value['thumbnailUrl']) ??
          _stringOrNull(_map(value['thumbnailMedia'])['downloadUrl']),
      note: _stringOrNull(value['note']),
      scheduledDate: _stringOrNull(value['scheduledDate']),
      durationSec: _nullablePositiveInt(value['durationSec']),
    );
  }

  Future<void> approveReview(String submissionId) async {
    if (!config.hasRemoteContext || submissionId.isEmpty) return;
    await apiClient.postJson(
      '/reviews',
      <String, Object?>{
        'submissionId': submissionId,
        'decision': 'approved',
        'feedback': <String, Object?>{
          'emoji': 'heart',
          'text': '看到了，完成得很好。',
        },
      },
      accessToken: config.accessToken,
      idempotencyKey: 'mobile-review-approve-$submissionId',
    );
  }

  Future<void> featureMemory(String memoryId) async {
    if (!config.hasRemoteContext || memoryId.isEmpty) return;
    await apiClient.postJson(
      '/memories/$memoryId/feature',
      const <String, Object?>{},
      accessToken: config.accessToken,
      idempotencyKey: 'mobile-feature-memory-$memoryId',
    );
  }

  Future<void> requestRevision(String submissionId, {String? feedback}) async {
    if (!config.hasRemoteContext || submissionId.isEmpty) return;
    await apiClient.postJson(
      '/reviews',
      <String, Object?>{
        'submissionId': submissionId,
        'decision': 'needs_revision',
        'feedback': <String, Object?>{
          'emoji': 'seed',
          'text': feedback?.trim().isNotEmpty == true
              ? feedback!.trim()
              : '请补做后再提交。',
        },
      },
      accessToken: config.accessToken,
      idempotencyKey: 'mobile-review-revision-$submissionId',
    );
  }

  Future<void> submitManualTask(ChildTask task) async {
    if (!config.hasRemoteContext || task.id.isEmpty) return;
    if (task.submissionTypeCode != 'manual') {
      throw StateError('This task requires media before submission.');
    }
    await _createSubmission(task.id, const <String>[],
        'mobile-manual-${task.id}-${DateTime.now().millisecondsSinceEpoch}');
  }

  Future<MediaSubmitOutcome> submitMediaTask({
    required ChildTask task,
    required File file,
    required String contentType,
  }) async {
    if (!config.hasRemoteContext || task.id.isEmpty)
      return MediaSubmitOutcome.completed;
    if (task.submissionTypeCode == 'manual') {
      throw StateError('Manual tasks do not accept media assets.');
    }
    final clientMutationId = stableClientMutationId(task.id, file);
    final queued = PendingUpload(
      clientMutationId: clientMutationId,
      taskInstanceId: task.id,
      file: file,
      contentType: contentType,
    );
    // The durable queue is the user-visible commit point.  Do not make the
    // capture page wait for a network connection before it can acknowledge
    // the save.  The worker below is deliberately detached; it records every
    // failure back into the same durable item for the home-page retry worker.
    await uploadQueue.enqueue(queued);
    unawaited(_processUpload(queued));
    return MediaSubmitOutcome.queued;
  }

  Future<MediaSubmitOutcome> retryPendingUploads({bool force = false}) async {
    await uploadQueue.load();
    final now = DateTime.now();
    MediaSubmitOutcome result = MediaSubmitOutcome.completed;
    for (final item in List<PendingUpload>.from(uploadQueue.items)) {
      if (!force &&
          item.nextAttemptAt != null &&
          item.nextAttemptAt!.isAfter(now)) continue;
      final current = await _processUpload(item);
      if (current != MediaSubmitOutcome.completed) result = current;
    }
    return result;
  }

  Future<void> resetUploadBackoff() => uploadQueue.resetBackoff();

  Future<MediaSubmitOutcome> _processUpload(PendingUpload item) async {
    if (_processing.contains(item.clientMutationId))
      return MediaSubmitOutcome.inProgress;
    _processing.add(item.clientMutationId);
    try {
      if (!await item.file.exists()) {
        await uploadQueue.update(item.copyWith(
          state: PendingUploadState.failed,
          attempts: item.attempts + 1,
          lastError: '媒体文件已不存在，请重新采集',
          nextAttemptAt: DateTime.now().add(const Duration(minutes: 5)),
        ));
        return MediaSubmitOutcome.failed;
      }
      await uploadQueue.update(item.copyWith(
          state: PendingUploadState.uploading, clearLastError: true));
      var mediaId = item.mediaAssetId;
      var uploadUrl = item.uploadUrl;
      final now = DateTime.now();
      if ((mediaId == null || mediaId.isEmpty) &&
          item.sessionRequestedAt != null &&
          now.difference(item.sessionRequestedAt!).inSeconds < 30) {
        return await _recordFailure(item, '上传会话正在冷却，请稍后手动重试');
      }
      if (mediaId == null ||
          mediaId.isEmpty ||
          uploadUrl == null ||
          uploadUrl.isEmpty) {
        await uploadQueue.update(item.copyWith(sessionRequestedAt: now));
        final uploadSession = await apiClient.postJson(
          '/media/upload-sessions',
          <String, Object?>{
            'familyId': config.familyId,
            'childId': config.childId,
            'purpose': 'submission',
            'contentType': item.contentType,
            'sizeBytes': await item.file.length(),
            'relatedResource': <String, Object?>{
              'type': 'task_instance',
              'id': item.taskInstanceId,
            },
          },
          accessToken: config.accessToken,
          idempotencyKey: 'mobile-upload-session-${item.clientMutationId}',
        );
        mediaId = _string(uploadSession['mediaId'], '');
        uploadUrl = _string(uploadSession['uploadUrl'], '');
        if (mediaId.isEmpty || uploadUrl.isEmpty)
          throw const FormatException('上传会话响应不完整');
        final expiresAt =
            DateTime.tryParse(uploadSession['expiresAt']?.toString() ?? '');
        await uploadQueue.update(item.copyWith(
          mediaAssetId: mediaId,
          uploadUrl: uploadUrl,
          uploadUrlExpiresAt: expiresAt,
          sessionRequestedAt: now,
        ));
      }
      final readyMediaId = mediaId;
      if (readyMediaId.isEmpty) {
        throw const FormatException('媒体资源未创建');
      }
      if (uploadUrl.isNotEmpty) {
        await apiClient.putFileToUrl(uploadUrl, item.file,
            contentType: item.contentType);
      }
      await apiClient.postJson(
        '/media/$readyMediaId/finalize',
        const <String, Object?>{},
        accessToken: config.accessToken,
        idempotencyKey: 'mobile-finalize-${item.clientMutationId}',
      );
      await _createSubmission(
          item.taskInstanceId, <String>[readyMediaId], item.clientMutationId);
      await uploadQueue.markCompleted(item.clientMutationId);
      uploadQueue.publishNotice('已提交，等家长看看');
      return MediaSubmitOutcome.completed;
    } on WishPoolApiException catch (error) {
      if (error.statusCode == 409) {
        await uploadQueue.markCompleted(item.clientMutationId);
        uploadQueue.publishNotice('这个任务已经打卡过啦');
        return MediaSubmitOutcome.alreadySubmitted;
      }
      return _recordFailure(item, _friendlyError(error));
    } on SocketException catch (_) {
      return _recordFailure(item, '当前离线，已保存，联网后自动上传');
    } on TimeoutException catch (_) {
      return _recordFailure(item, '网络暂时不稳定，已保存，稍后自动重试');
    } on HttpException catch (_) {
      return _recordFailure(item, '网络暂时不稳定，已保存，稍后自动重试');
    } catch (_) {
      return _recordFailure(item, '上传暂时失败，已保存，可稍后重试');
    } finally {
      _processing.remove(item.clientMutationId);
    }
  }

  Future<MediaSubmitOutcome> _recordFailure(
      PendingUpload item, String message) async {
    await uploadQueue.load();
    final matches = uploadQueue.items.where(
        (candidate) => candidate.clientMutationId == item.clientMutationId);
    if (matches.isNotEmpty) item = matches.first;
    final attempts = item.attempts + 1;
    final terminal = attempts >= maxUploadAttempts;
    final seconds = (1 << attempts.clamp(0, 8)).clamp(2, 300).toInt();
    await uploadQueue.update(item.copyWith(
      state: PendingUploadState.failed,
      attempts: attempts,
      lastError: message,
      nextAttemptAt:
          terminal ? null : DateTime.now().add(Duration(seconds: seconds)),
      clearNextAttempt: terminal,
    ));
    uploadQueue
        .publishNotice(terminal ? '上传多次失败，已暂停自动重试，请点击手动重试' : '网络不稳定，稍后自动重试');
    return MediaSubmitOutcome.failed;
  }

  String _friendlyError(WishPoolApiException error) {
    if (error.statusCode >= 500 ||
        error.statusCode == 408 ||
        error.statusCode == 429) {
      return '网络暂时不稳定，已保存，稍后自动重试';
    }
    return '提交失败，请稍后重试';
  }

  Future<Map<String, Object?>> findWishImageCandidates({
    required String title,
    String? note,
    int limit = 6,
  }) async {
    if (!config.hasRemoteContext || title.trim().isEmpty)
      return const <String, Object?>{'items': <Object?>[]};
    return apiClient.postJson(
      '/wishes/image-candidates',
      <String, Object?>{
        'familyId': config.familyId,
        'childId': config.childId,
        'title': title,
        'note': note,
        'limit': limit,
      },
      accessToken: config.accessToken,
    );
  }

  Future<String> uploadWishImage({
    required File file,
    required String contentType,
  }) async {
    if (!config.hasRemoteContext)
      throw StateError('Remote context is required.');
    if (!contentType.startsWith('image/'))
      throw StateError('Wish image upload requires an image file.');
    final clientMutationId =
        'mobile-wish-image-${DateTime.now().millisecondsSinceEpoch}';
    final uploadSession = await apiClient.postJson(
      '/media/upload-sessions',
      <String, Object?>{
        'familyId': config.familyId,
        'childId': config.childId,
        'purpose': 'wish_image',
        'contentType': contentType,
        'sizeBytes': await file.length(),
      },
      accessToken: config.accessToken,
      idempotencyKey: 'mobile-wish-image-upload-session-$clientMutationId',
    );
    final mediaId = _string(uploadSession['mediaId'], '');
    final uploadUrl = _string(uploadSession['uploadUrl'], '');
    if (mediaId.isEmpty || uploadUrl.isEmpty)
      throw const FormatException('Upload session response is incomplete.');
    await apiClient.putFileToUrl(uploadUrl, file, contentType: contentType);
    await apiClient.postJson(
      '/media/$mediaId/finalize',
      <String, Object?>{'checksumSha256': await checksumSha256(file)},
      accessToken: config.accessToken,
      idempotencyKey: 'mobile-wish-image-finalize-$mediaId',
    );
    return mediaId;
  }

  Future<void> arrangeRoomItem(RoomItemData item,
      {required double left, required double top}) async {
    if (!config.hasRemoteContext || item.id.isEmpty) return;
    await apiClient.postJson(
      '/room/items/${item.id}/arrange',
      <String, Object?>{
        'position': <String, Object?>{
          'x': left,
          'y': top,
        },
      },
      accessToken: config.accessToken,
      idempotencyKey:
          'mobile-room-arrange-${item.id}-${DateTime.now().millisecondsSinceEpoch}',
    );
  }

  Future<void> skipTask(ChildTask task) async {
    if (!config.hasRemoteContext || task.id.isEmpty) return;
    await apiClient.postJson(
      '/tasks/${task.id}/skip',
      const <String, Object?>{
        'reason': '家庭临时调整',
      },
      accessToken: config.accessToken,
      idempotencyKey:
          'mobile-task-skip-${task.id}-${DateTime.now().millisecondsSinceEpoch}',
    );
  }

  Future<void> postponeTask(ChildTask task) async {
    if (!config.hasRemoteContext || task.id.isEmpty) return;
    final newDate = DateTime.now()
        .add(const Duration(days: 1))
        .toIso8601String()
        .substring(0, 10);
    await apiClient.postJson(
      '/tasks/${task.id}/postpone',
      <String, Object?>{
        'newDate': newDate,
        'reason': '顺延到下一天',
      },
      accessToken: config.accessToken,
      idempotencyKey:
          'mobile-task-postpone-${task.id}-${DateTime.now().millisecondsSinceEpoch}',
    );
  }

  Future<void> markNotificationsRead(List<String> notificationIds) async {
    if (!config.hasRemoteContext || notificationIds.isEmpty) return;
    await apiClient.postJson(
      '/notifications/read',
      <String, Object?>{
        'notificationIds': notificationIds,
      },
      accessToken: config.accessToken,
    );
  }

  Future<void> updateNotificationPreference(
      NotificationPreferenceData preference,
      {required bool enabled}) async {
    if (!config.hasRemoteContext || config.familyId.isEmpty) return;
    await apiClient.putJson(
      '/notification-preferences',
      <String, Object?>{
        'familyId': config.familyId,
        'notificationType': preference.type,
        'enabled': enabled,
        'channels': <String, Object?>{
          'inbox': true,
          'push': false,
        },
      },
      accessToken: config.accessToken,
    );
  }

  List<ChildTask> _tasks(Object? value,
      {bool fallbackOnEmpty = true, ChildFeedbackData? latestFeedback}) {
    if (value is! List)
      return fallbackOnEmpty ? fixtureSnapshot.childTasks : const <ChildTask>[];
    final tasks = value.whereType<Map>().map((task) {
      return ChildTask(
        id: _string(task['id'], ''),
        title: _string(task['title'], '未命名任务'),
        category: _categoryLabel(_string(task['category'], 'custom')),
        status: _taskStatusLabel(_string(task['status'], 'todo')),
        // The server is the only source of truth. Older responses do not
        // expose the ledger amount, so zero means “not provided”, never a
        // guessed star value.
        reward: _int(task['rewardAmount'],
            _int(task['rewardStarlight'], _int(task['reward'], 0))),
        submissionType:
            _submissionTypeLabel(_string(task['submissionType'], 'manual')),
        submissionTypeCode: _string(task['submissionType'], 'manual'),
        statusCode: _string(task['status'], 'todo'),
        feedback: latestFeedback != null &&
                latestFeedback.taskTitle == _string(task['title'], '')
            ? latestFeedback.text
            : null,
      );
    }).toList();
    return tasks.isEmpty && fallbackOnEmpty
        ? fixtureSnapshot.childTasks
        : tasks;
  }

  List<ReviewCardData> _reviews(Object? value, {bool fallbackOnEmpty = true}) {
    if (value is! List)
      return fallbackOnEmpty
          ? fixtureSnapshot.reviewCards
          : const <ReviewCardData>[];
    final reviews = value.whereType<Map>().map((card) {
      final task = _map(card['task']);
      final submission = _map(card['submission']);
      return ReviewCardData(
        submissionId: _string(submission['id'], ''),
        title: _string(task['title'], '待审核提交'),
        summary: _string(card['aiSummary'], 'AI 预审暂未完成，请查看原始提交内容。'),
        mediaType: _submissionTypeLabel(
            _string(submission['submissionType'], 'manual')),
      );
    }).toList();
    return reviews.isEmpty && fallbackOnEmpty
        ? fixtureSnapshot.reviewCards
        : reviews;
  }

  List<WeeklyPlanRuleData> _planRules(Object? value,
      {bool fallbackOnEmpty = true}) {
    if (value is! List)
      return fallbackOnEmpty
          ? fixtureSnapshot.weeklyPlanRules
          : const <WeeklyPlanRuleData>[];
    final rules = value.whereType<Map>().map((rule) {
      return WeeklyPlanRuleData(
        title: _string(rule['title'], '未命名任务'),
        category: _categoryLabel(_string(rule['category'], 'custom')),
        submissionType:
            _submissionTypeLabel(_string(rule['submissionType'], 'manual')),
        weekdays: _weekdays(rule['weekdays']),
        isCore: rule['isCore'] == true,
      );
    }).toList();
    return rules.isEmpty && fallbackOnEmpty
        ? fixtureSnapshot.weeklyPlanRules
        : rules;
  }

  List<RoomItemData> _roomItems(Object? value, {bool fallbackOnEmpty = true}) {
    if (value is! List)
      return fallbackOnEmpty
          ? fixtureSnapshot.roomItems
          : const <RoomItemData>[];
    final items = value.whereType<Map>().map((item) {
      final position = _map(item['position']);
      return RoomItemData(
        id: _string(item['id'], ''),
        title: _string(item['title'], '小屋元素'),
        left: _num(position['left'], _num(position['x'], 20)) * 1.0,
        top: _num(position['top'], _num(position['y'], 80)) * 1.0,
        unlocked: item['visible'] != false,
      );
    }).toList();
    return items.isEmpty && fallbackOnEmpty ? fixtureSnapshot.roomItems : items;
  }

  List<NotificationItemData> _notifications(Object? value,
      {bool fallbackOnEmpty = true}) {
    if (value is! List)
      return fallbackOnEmpty
          ? fixtureSnapshot.notificationInbox
          : const <NotificationItemData>[];
    final items = value.whereType<Map>().map((item) {
      return NotificationItemData(
        id: _string(item['id'], ''),
        title: _string(item['title'], '通知'),
        body: _string(item['body'], ''),
        status: _string(item['status'], 'pending'),
        createdAt: _string(item['createdAt'], ''),
      );
    }).toList();
    return items.isEmpty && fallbackOnEmpty
        ? fixtureSnapshot.notificationInbox
        : items;
  }

  List<NotificationPreferenceData> _preferences(Object? value) {
    if (value is! List) return fixtureSnapshot.notificationPreferences;
    final preferences = value.whereType<Map>().map((item) {
      return NotificationPreferenceData(
        type: _string(item['notificationType'], 'task_plan_changed'),
        enabled: item['enabled'] != false,
      );
    }).toList();
    return preferences.isEmpty
        ? fixtureSnapshot.notificationPreferences
        : preferences;
  }

  double _wishProgress(Map<Object?, Object?> wish,
      {bool fallbackOnEmpty = true}) {
    final required = _num(wish['requiredFragments'], 0);
    if (required <= 0)
      return fallbackOnEmpty ? fixtureSnapshot.wishProgress : 0;
    final earned = _num(wish['earnedFragments'], 0);
    return (earned / required).clamp(0, 1).toDouble();
  }

  WishFragmentMaskData? _fragmentMask(Object? value) {
    final mask = _map(value);
    final rows = _nullablePositiveInt(mask['rows']);
    final cols = _nullablePositiveInt(mask['cols']);
    final total = _nullablePositiveInt(mask['total']);
    final rawCells = mask['cells'];
    if (rows == null ||
        cols == null ||
        total == null ||
        rows * cols != total ||
        rawCells is! List) return null;
    final cells = rawCells
        .whereType<Map>()
        .map((cell) {
          final index = _int(cell['index'], -1);
          return WishFragmentCellData(
            index: index,
            row: _int(cell['row'], index >= 0 ? index ~/ cols : 0),
            col: _int(cell['col'], index >= 0 ? index % cols : 0),
            polygon: _fragmentPolygon(cell['polygon']),
          );
        })
        .where((cell) => cell.index >= 0 && cell.index < total)
        .toList()
      ..sort((a, b) => a.index.compareTo(b.index));
    if (cells.length != total) return null;
    return WishFragmentMaskData(
      version: _positiveInt(mask['version'], 1),
      mode: _string(mask['mode'], 'grid_reveal'),
      rows: rows,
      cols: cols,
      total: total,
      revealOrder: _intList(mask['revealOrder']),
      cells: cells,
    );
  }

  List<WishFragmentPointData>? _fragmentPolygon(Object? value) {
    if (value is! List) return null;
    final points = value.whereType<Map>().map((point) {
      return WishFragmentPointData(
        x: _num(point['x'], 0).clamp(0, 1).toDouble(),
        y: _num(point['y'], 0).clamp(0, 1).toDouble(),
      );
    }).toList();
    return points.length >= 3 ? points : null;
  }

  List<int> _intList(Object? value) {
    if (value is! List) return const <int>[];
    return value
        .whereType<num>()
        .map((item) => item.toInt())
        .where((item) => item >= 0)
        .toSet()
        .toList()
      ..sort();
  }

  Map<Object?, Object?> _map(Object? value) => value is Map ? value : const {};

  String _string(Object? value, String fallback) =>
      value is String && value.isNotEmpty ? value : fallback;

  String? _stringOrNull(Object? value) =>
      value is String && value.isNotEmpty ? value : null;

  int _int(Object? value, int fallback) =>
      value is num ? value.toInt() : fallback;

  int _positiveInt(Object? value, int fallback) {
    final parsed = _int(value, fallback);
    return parsed > 0 ? parsed : fallback;
  }

  int? _nullablePositiveInt(Object? value) {
    final parsed = _int(value, 0);
    return parsed > 0 ? parsed : null;
  }

  double _num(Object? value, double fallback) =>
      value is num ? value.toDouble() : fallback;

  List<int> _weekdays(Object? value) {
    if (value is! List) return const <int>[];
    return value
        .whereType<num>()
        .map((day) => day.toInt())
        .where((day) => day >= 1 && day <= 7)
        .toList();
  }

  String _taskStatusLabel(String status) {
    return switch (status) {
      'approved' => '已通过',
      'pending_review' => '待家长审核',
      'submitted' || 'ai_processing' => '已提交·AI 预审中',
      'needs_revision' => '被退回',
      'expired' || 'overdue' || 'skipped' => '已过期/未完成',
      _ => '待打卡',
    };
  }

  ChildFeedbackData? _feedback(Object? value) {
    final data = _map(value);
    if (data.isEmpty) return null;
    return ChildFeedbackData(
      taskTitle: _string(data['taskTitle'], ''),
      decision: _string(data['decision'], ''),
      text: _stringOrNull(data['text']),
      createdAt: _string(data['createdAt'], ''),
    );
  }

  String _categoryLabel(String category) {
    return switch (category) {
      'study' => '学习',
      'reading' => '朗读',
      'exercise' => '运动',
      'habit' => '习惯',
      _ => '自定义',
    };
  }

  String _submissionTypeLabel(String submissionType) {
    return switch (submissionType) {
      'photo' => '照片',
      'audio' => '语音',
      'video' => '视频',
      _ => '确认',
    };
  }

  Future<void> _createSubmission(String taskInstanceId,
      List<String> mediaAssetIds, String clientMutationId) async {
    await apiClient.postJson(
      '/submissions',
      <String, Object?>{
        'taskInstanceId': taskInstanceId,
        'clientMutationId': clientMutationId,
        'mediaAssetIds': mediaAssetIds,
        'submittedAtClient': DateTime.now().toUtc().toIso8601String(),
      },
      accessToken: config.accessToken,
      idempotencyKey: 'mobile-submission-$clientMutationId',
    );
  }
}
