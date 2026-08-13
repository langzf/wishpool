import 'dart:io';

import '../domain/wishpool_snapshot.dart';
import '../infrastructure/runtime_config.dart';
import '../infrastructure/wishpool_api_client.dart';
import '../shared/fixture_data.dart';
import 'upload_queue.dart';

class WishPoolRepository {
  const WishPoolRepository({
    required this.config,
    required this.apiClient,
    required this.uploadQueue,
  });

  final WishPoolRuntimeConfig config;
  final WishPoolApiClient apiClient;
  final UploadQueue uploadQueue;

  Future<WishPoolSnapshot> loadHomeSnapshot() async {
    if (!config.hasRemoteContext) return fixtureSnapshot;
    try {
      final childHome = await apiClient.getJson(
        '/children/${config.childId}/home-context',
        accessToken: config.accessToken,
      );
      final parentHome = config.isChildDevice ? const <String, Object?>{} : await _parentHome();
      final notifications = await _notificationInbox();
      final preferences = await _notificationPreferences();
      final currentWish = _map(childHome['currentWish']);
      return WishPoolSnapshot(
        source: 'core-api',
        childName: _string(_map(childHome['child'])['nickname'], fixtureSnapshot.childName),
        familyName: _string(_map(parentHome['family'])['name'], ''),
        wishTitle: _string(currentWish['title'], '还没有本周心愿'),
        wishProgress: _wishProgress(currentWish, fallbackOnEmpty: false),
        wishCurrentFragments: _int(currentWish['earnedFragments'], 0),
        wishTargetFragments: _positiveInt(currentWish['requiredFragments'], 1),
        childTasks: _tasks(_map(childHome['today'])['tasks'], fallbackOnEmpty: false),
        reviewCards: config.isChildDevice ? const <ReviewCardData>[] : _reviews(parentHome['pendingReviews'], fallbackOnEmpty: false),
        weeklyPlanRules: config.isChildDevice
            ? const <WeeklyPlanRuleData>[]
            : _planRules(_map(parentHome['weeklyPlan'])['rules'], fallbackOnEmpty: false),
        roomItems: _roomItems(_map(childHome['room'])['items'], fallbackOnEmpty: false),
        latestMemoryTitle: _stringOrNull(_map(childHome['latestMemory'])['title']),
        unreadNotifications: _int(childHome['unreadNotifications'], 0),
        notificationInbox: _notifications(notifications['items'], fallbackOnEmpty: false),
        notificationPreferences: _preferences(preferences),
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

  Future<void> requestRevision(String submissionId) async {
    if (!config.hasRemoteContext || submissionId.isEmpty) return;
    await apiClient.postJson(
      '/reviews',
      <String, Object?>{
        'submissionId': submissionId,
        'decision': 'needs_revision',
        'feedback': <String, Object?>{
          'emoji': 'seed',
          'text': '再补充一点点，我们一起做得更完整。',
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
    await _createSubmission(task.id, const <String>[], 'mobile-manual-${task.id}-${DateTime.now().millisecondsSinceEpoch}');
  }

  Future<void> submitMediaTask({
    required ChildTask task,
    required File file,
    required String contentType,
  }) async {
    if (!config.hasRemoteContext || task.id.isEmpty) return;
    if (task.submissionTypeCode == 'manual') {
      throw StateError('Manual tasks do not accept media assets.');
    }
    final clientMutationId = 'mobile-media-${task.id}-${DateTime.now().millisecondsSinceEpoch}';
    uploadQueue.enqueue(
      PendingUpload(
        clientMutationId: clientMutationId,
        taskInstanceId: task.id,
        file: file,
        contentType: contentType,
      ),
    );

    final uploadSession = await apiClient.postJson(
      '/media/upload-sessions',
      <String, Object?>{
        'familyId': config.familyId,
        'childId': config.childId,
        'purpose': 'submission',
        'contentType': contentType,
        'sizeBytes': await file.length(),
        'relatedResource': <String, Object?>{
          'type': 'task_instance',
          'id': task.id,
        },
      },
      accessToken: config.accessToken,
      idempotencyKey: 'mobile-upload-session-$clientMutationId',
    );
    final mediaId = _string(uploadSession['mediaId'], '');
    final uploadUrl = _string(uploadSession['uploadUrl'], '');
    if (mediaId.isEmpty || uploadUrl.isEmpty) throw const FormatException('Upload session response is incomplete.');

    await apiClient.putFileToUrl(uploadUrl, file, contentType: contentType);
    await apiClient.postJson(
      '/media/$mediaId/finalize',
      const <String, Object?>{},
      accessToken: config.accessToken,
      idempotencyKey: 'mobile-finalize-$clientMutationId',
    );
    await _createSubmission(task.id, <String>[mediaId], clientMutationId);
    uploadQueue.markCompleted(clientMutationId);
  }

  Future<void> arrangeRoomItem(RoomItemData item, {required double left, required double top}) async {
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
      idempotencyKey: 'mobile-room-arrange-${item.id}-${DateTime.now().millisecondsSinceEpoch}',
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
      idempotencyKey: 'mobile-task-skip-${task.id}-${DateTime.now().millisecondsSinceEpoch}',
    );
  }

  Future<void> postponeTask(ChildTask task) async {
    if (!config.hasRemoteContext || task.id.isEmpty) return;
    final newDate = DateTime.now().add(const Duration(days: 1)).toIso8601String().substring(0, 10);
    await apiClient.postJson(
      '/tasks/${task.id}/postpone',
      <String, Object?>{
        'newDate': newDate,
        'reason': '顺延到下一天',
      },
      accessToken: config.accessToken,
      idempotencyKey: 'mobile-task-postpone-${task.id}-${DateTime.now().millisecondsSinceEpoch}',
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

  Future<void> updateNotificationPreference(NotificationPreferenceData preference, {required bool enabled}) async {
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

  List<ChildTask> _tasks(Object? value, {bool fallbackOnEmpty = true}) {
    if (value is! List) return fallbackOnEmpty ? fixtureSnapshot.childTasks : const <ChildTask>[];
    final tasks = value.whereType<Map>().map((task) {
      return ChildTask(
        id: _string(task['id'], ''),
        title: _string(task['title'], '未命名任务'),
        category: _categoryLabel(_string(task['category'], 'custom')),
        status: _taskStatusLabel(_string(task['status'], 'todo')),
        reward: task['isCore'] == true ? 8 : 4,
        submissionType: _submissionTypeLabel(_string(task['submissionType'], 'manual')),
        submissionTypeCode: _string(task['submissionType'], 'manual'),
      );
    }).toList();
    return tasks.isEmpty && fallbackOnEmpty ? fixtureSnapshot.childTasks : tasks;
  }

  List<ReviewCardData> _reviews(Object? value, {bool fallbackOnEmpty = true}) {
    if (value is! List) return fallbackOnEmpty ? fixtureSnapshot.reviewCards : const <ReviewCardData>[];
    final reviews = value.whereType<Map>().map((card) {
      final task = _map(card['task']);
      final submission = _map(card['submission']);
      return ReviewCardData(
        submissionId: _string(submission['id'], ''),
        title: _string(task['title'], '待审核提交'),
        summary: _string(card['aiSummary'], 'AI 预审暂未完成，请查看原始提交内容。'),
        mediaType: _submissionTypeLabel(_string(submission['submissionType'], 'manual')),
      );
    }).toList();
    return reviews.isEmpty && fallbackOnEmpty ? fixtureSnapshot.reviewCards : reviews;
  }

  List<WeeklyPlanRuleData> _planRules(Object? value, {bool fallbackOnEmpty = true}) {
    if (value is! List) return fallbackOnEmpty ? fixtureSnapshot.weeklyPlanRules : const <WeeklyPlanRuleData>[];
    final rules = value.whereType<Map>().map((rule) {
      return WeeklyPlanRuleData(
        title: _string(rule['title'], '未命名任务'),
        category: _categoryLabel(_string(rule['category'], 'custom')),
        submissionType: _submissionTypeLabel(_string(rule['submissionType'], 'manual')),
        weekdays: _weekdays(rule['weekdays']),
        isCore: rule['isCore'] == true,
      );
    }).toList();
    return rules.isEmpty && fallbackOnEmpty ? fixtureSnapshot.weeklyPlanRules : rules;
  }

  List<RoomItemData> _roomItems(Object? value, {bool fallbackOnEmpty = true}) {
    if (value is! List) return fallbackOnEmpty ? fixtureSnapshot.roomItems : const <RoomItemData>[];
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

  List<NotificationItemData> _notifications(Object? value, {bool fallbackOnEmpty = true}) {
    if (value is! List) return fallbackOnEmpty ? fixtureSnapshot.notificationInbox : const <NotificationItemData>[];
    final items = value.whereType<Map>().map((item) {
      return NotificationItemData(
        id: _string(item['id'], ''),
        title: _string(item['title'], '通知'),
        body: _string(item['body'], ''),
        status: _string(item['status'], 'pending'),
        createdAt: _string(item['createdAt'], ''),
      );
    }).toList();
    return items.isEmpty && fallbackOnEmpty ? fixtureSnapshot.notificationInbox : items;
  }

  List<NotificationPreferenceData> _preferences(Object? value) {
    if (value is! List) return fixtureSnapshot.notificationPreferences;
    final preferences = value.whereType<Map>().map((item) {
      return NotificationPreferenceData(
        type: _string(item['notificationType'], 'task_plan_changed'),
        enabled: item['enabled'] != false,
      );
    }).toList();
    return preferences.isEmpty ? fixtureSnapshot.notificationPreferences : preferences;
  }

  double _wishProgress(Map<Object?, Object?> wish, {bool fallbackOnEmpty = true}) {
    final required = _num(wish['requiredFragments'], 0);
    if (required <= 0) return fallbackOnEmpty ? fixtureSnapshot.wishProgress : 0;
    final earned = _num(wish['earnedFragments'], 0);
    return (earned / required).clamp(0, 1).toDouble();
  }

  Map<Object?, Object?> _map(Object? value) => value is Map ? value : const {};

  String _string(Object? value, String fallback) => value is String && value.isNotEmpty ? value : fallback;

  String? _stringOrNull(Object? value) => value is String && value.isNotEmpty ? value : null;

  int _int(Object? value, int fallback) => value is num ? value.toInt() : fallback;

  int _positiveInt(Object? value, int fallback) {
    final parsed = _int(value, fallback);
    return parsed > 0 ? parsed : fallback;
  }

  double _num(Object? value, double fallback) => value is num ? value.toDouble() : fallback;

  List<int> _weekdays(Object? value) {
    if (value is! List) return const <int>[];
    return value.whereType<num>().map((day) => day.toInt()).where((day) => day >= 1 && day <= 7).toList();
  }

  String _taskStatusLabel(String status) {
    return switch (status) {
      'approved' => '已通过',
      'pending_review' => '待审核',
      'ai_processing' => 'AI 预审中',
      'needs_revision' => '需修改',
      'skipped' => '已跳过',
      _ => '待打卡',
    };
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

  Future<void> _createSubmission(String taskInstanceId, List<String> mediaAssetIds, String clientMutationId) async {
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
