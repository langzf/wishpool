import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';

import '../data/sync_coordinator.dart';
import '../data/upload_queue.dart';
import '../data/wishpool_repository.dart';
import '../data/wishpool_scope.dart';
import '../design/wishpool_theme.dart';
import '../domain/wishpool_snapshot.dart';
import '../shared/fixture_data.dart';
import '../infrastructure/wishpool_api_client.dart';
import 'child_dashboard.dart';
import 'notification_screen.dart';
import 'memory_timeline_screen.dart';
import 'parent_dashboard.dart';
import 'room_screen.dart';
import 'wish_screen.dart';
import '../media/capture_screen.dart';

class MobileHomeScreen extends StatefulWidget {
  const MobileHomeScreen({
    super.key,
    this.childMode = false,
    this.onSignOut,
    this.onChildModeLockSettings,
  });

  final bool childMode;
  final VoidCallback? onSignOut;
  final VoidCallback? onChildModeLockSettings;

  @override
  State<MobileHomeScreen> createState() => _MobileHomeScreenState();
}

class _MobileHomeScreenState extends State<MobileHomeScreen>
    with WidgetsBindingObserver {
  int _index = 0;
  late Future<WishPoolSnapshot> _snapshotFuture;
  StreamSubscription? _syncSubscription;
  Timer? _uploadRetryTimer;
  Timer? _networkRecoveryTimer;
  bool _uploadRetrying = false;
  bool _isDisposing = false;
  UploadQueue? _uploadQueue;
  bool _networkRecoveryInFlight = false;
  bool _retryAfterCurrentUpload = false;
  bool _isForeground = true;
  final _networkRecoveryBackoff = NetworkRecoveryBackoff();
  int _lastNoticeVersion = 0;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final queue = WishPoolScope.of(context).uploadQueue;
    _uploadQueue = queue;
    queue.removeListener(_handleQueueNotice);
    queue.addListener(_handleQueueNotice);
    _snapshotFuture = WishPoolScope.of(context).loadSnapshot();
    _startRealtimeSync();
    _startUploadRetry();
    _startNetworkRecoveryMonitor();
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    _isDisposing = true;
    _uploadQueue?.removeListener(_handleQueueNotice);
    _syncSubscription?.cancel();
    _uploadRetryTimer?.cancel();
    _networkRecoveryTimer?.cancel();
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  void _handleQueueNotice() {
    if (!mounted || _isDisposing) return;
    final queue = WishPoolScope.of(context).uploadQueue;
    if (queue.noticeVersion == _lastNoticeVersion) return;
    _lastNoticeVersion = queue.noticeVersion;
    final notice = queue.lastNotice;
    if (notice == null) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(notice)));
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    final foreground = state == AppLifecycleState.resumed;
    _isForeground = foreground;
    if (!foreground) {
      _networkRecoveryTimer?.cancel();
      _networkRecoveryTimer = null;
      _uploadRetryTimer?.cancel();
      _uploadRetryTimer = null;
      return;
    }
    _networkRecoveryBackoff.reset();
    _retryUploads();
    _startUploadRetry();
    _startNetworkRecoveryMonitor();
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<WishPoolSnapshot>(
      future: _snapshotFuture,
      builder: (context, snapshot) {
        final scope = WishPoolScope.of(context);
        if (snapshot.hasError &&
            snapshot.error is WishPoolApiException &&
            (snapshot.error as WishPoolApiException).statusCode == 401) {
          return Scaffold(
              body: Center(
                  child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Column(mainAxisSize: MainAxisSize.min, children: [
                        const Text('设备已被移除，请重新配对', textAlign: TextAlign.center),
                        const SizedBox(height: 16),
                        FilledButton(
                            onPressed: widget.onSignOut,
                            child: const Text('重新配对')),
                      ]))));
        }
        final data = snapshot.data ?? fixtureSnapshot;
        final screens = <Widget>[
          ChildDashboardScreen(
            snapshot: data,
            onStartTask: (task) => _showTaskSheet(context, data, task),
          ),
          WishScreen(snapshot: data),
          NotificationScreen(
            snapshot: data,
            showPreferences: !widget.childMode,
            onChanged: _reloadSnapshot,
          ),
          MemoryTimelineScreen(
            snapshot: data,
            onFeature: (memoryId) async {
              await scope.repository.featureMemory(memoryId);
              if (context.mounted) {
                ScaffoldMessenger.of(context)
                    .showSnackBar(const SnackBar(content: Text('已精选入屋')));
                _reloadSnapshot();
              }
            },
          ),
          RoomScreen(
            snapshot: data,
            onRoomChanged: _reloadSnapshot,
            onSignOut: widget.childMode ? widget.onSignOut : null,
          ),
          if (!widget.childMode)
            ParentDashboardScreen(
              snapshot: data,
              onSignOut: widget.onSignOut,
              onSwitchToChildDevice: widget.onSignOut,
              onDataChanged: _reloadSnapshot,
              onChildModeLockSettings: widget.onChildModeLockSettings,
            ),
        ];
        final destinations = <NavigationDestination>[
          const NavigationDestination(
              icon: Icon(Icons.check_circle_outline), label: '任务'),
          const NavigationDestination(
              icon: Icon(Icons.favorite_border), label: '心愿'),
          const NavigationDestination(
              icon: Icon(Icons.notifications_none), label: '通知'),
          const NavigationDestination(
              icon: Icon(Icons.auto_stories_outlined), label: '回忆'),
          const NavigationDestination(
              icon: Icon(Icons.home_outlined), label: '小屋'),
          if (!widget.childMode)
            const NavigationDestination(
                icon: Icon(Icons.verified_outlined), label: '家长'),
        ];
        final selectedIndex = _index.clamp(0, screens.length - 1);
        return Scaffold(
          body: SafeArea(
            child: AnimatedBuilder(
              animation: scope.uploadQueue,
              builder: (context, _) => Column(
                children: [
                  _UploadQueueBanner(
                    queue: scope.uploadQueue,
                    onRetry: () => _retryUploads(force: true),
                  ),
                  Expanded(child: screens[selectedIndex]),
                ],
              ),
            ),
          ),
          bottomNavigationBar: NavigationBar(
            selectedIndex: selectedIndex,
            onDestinationSelected: (value) => setState(() => _index = value),
            destinations: destinations,
          ),
          floatingActionButton: selectedIndex == 0
              ? FloatingActionButton.extended(
                  onPressed: () => _showCheckInSheet(context, data),
                  icon: const Icon(Icons.add_a_photo_outlined),
                  label: const Text('打卡'),
                  backgroundColor: WishPoolColors.primary,
                  foregroundColor: WishPoolColors.onPrimary,
                )
              : null,
        );
      },
    );
  }

  void _reloadSnapshot() {
    setState(() {
      _snapshotFuture = WishPoolScope.of(context).loadSnapshot();
    });
  }

  void _startUploadRetry() {
    if (!_isForeground) return;
    final scope = WishPoolScope.of(context);
    scope.uploadQueue.load();
    _retryUploads();
    _uploadRetryTimer ??=
        Timer.periodic(const Duration(seconds: 10), (_) => _retryUploads());
  }

  void _startNetworkRecoveryMonitor() {
    if (!_isForeground || _networkRecoveryTimer != null) return;
    _scheduleNetworkRecoveryProbe();
  }

  void _scheduleNetworkRecoveryProbe() {
    if (!_isForeground || !mounted) return;
    _networkRecoveryTimer?.cancel();
    final hasPendingUploads = _uploadQueue?.items.isNotEmpty ?? false;
    _networkRecoveryTimer = Timer(
        _networkRecoveryBackoff.delayForQueue(
            hasPendingUploads: hasPendingUploads), () async {
      _networkRecoveryTimer = null;
      await _runNetworkRecoveryProbe();
      if (_isForeground && mounted) _scheduleNetworkRecoveryProbe();
    });
  }

  Future<void> _runNetworkRecoveryProbe() async {
    if (_networkRecoveryInFlight || !_isForeground || !mounted) return;
    final scope = WishPoolScope.of(context);
    final config = scope.config;
    if (!config.hasRemoteContext) return;
    _networkRecoveryInFlight = true;
    try {
      final uri = Uri.parse(config.coreApiBaseUrl);
      final port = uri.hasPort ? uri.port : (uri.scheme == 'https' ? 443 : 80);
      // DNS resolution is not a connectivity check: Android can answer it
      // from cache while airplane mode still has no route.  A TCP connect to
      // the actual Core API endpoint proves that the route is usable.
      final socket = await Socket.connect(uri.host, port,
          timeout: const Duration(seconds: 3));
      await socket.close();
      _networkRecoveryBackoff.reset();
      await UploadRecoveryHandler(
              scope.uploadQueue, () => _retryUploads(force: true))
          .onNetworkRecovered();
    } on SocketException {
      _increaseNetworkRecoveryDelay();
    } on TimeoutException {
      _increaseNetworkRecoveryDelay();
    } finally {
      _networkRecoveryInFlight = false;
    }
  }

  void _increaseNetworkRecoveryDelay() {
    _networkRecoveryBackoff.recordFailure(
        hasPendingUploads: _uploadQueue?.items.isNotEmpty ?? false);
  }

  Future<void> _retryUploads({bool force = false}) async {
    if (_uploadRetrying) {
      if (force) _retryAfterCurrentUpload = true;
      return;
    }
    if (!mounted || _isDisposing) return;
    final scope = WishPoolScope.of(context);
    if (!scope.config.hasRemoteContext) return;
    _uploadRetrying = true;
    try {
      final outcome = await scope.retryPendingUploads(force: force);
      if (mounted && outcome == MediaSubmitOutcome.alreadySubmitted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('这个任务已经打卡过啦')),
        );
      }
      if (mounted) _reloadSnapshot();
    } finally {
      _uploadRetrying = false;
      if (_retryAfterCurrentUpload && mounted && !_isDisposing) {
        _retryAfterCurrentUpload = false;
        unawaited(_retryUploads(force: true));
      }
    }
  }

  void _startRealtimeSync() {
    if (_syncSubscription != null) return;
    final scope = WishPoolScope.of(context);
    final config = scope.config;
    if (!config.hasRemoteContext) return;
    scope.syncCoordinator
        .start(familyId: config.familyId, accessToken: config.accessToken);
    _syncSubscription = scope.syncCoordinator.events.listen((event) {
      if (!_shouldRefreshForEvent(event)) return;
      if (!mounted) return;
      _reloadSnapshot();
    });
  }

  bool _shouldRefreshForEvent(FamilySyncEvent event) {
    return {
      'task.created',
      'task.skipped',
      'task.postponed',
      'submission.created',
      'review.approved',
      'review.needs_revision',
      'reward.issued',
      'wish.fragment_awarded',
      'wish.unlocked',
      'memory.weekly_card_generated',
      'room.item_unlocked',
      'room.item_arranged',
      'notification.created',
    }.contains(event.type);
  }

  void _showCheckInSheet(BuildContext context, WishPoolSnapshot snapshot) {
    final openTasks =
        snapshot.childTasks.where((task) => task.status != '已通过').toList();
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (context) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('选择今天的小冒险',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
            const SizedBox(height: WishPoolSpacing.md),
            if (openTasks.isEmpty)
              const ListTile(
                contentPadding: EdgeInsets.zero,
                leading: Icon(Icons.check_circle_outline),
                title: Text('今天没有可提交任务'),
                subtitle: Text('新的任务安排同步后会出现在这里。'),
              )
            else
              for (final task in openTasks)
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.add_a_photo_outlined),
                  title: Text(task.title),
                  subtitle: Text('${task.submissionType}提交 · ${task.status}'),
                  onTap: () {
                    Navigator.pop(context);
                    _showTaskSheet(context, snapshot, task);
                  },
                ),
          ],
        ),
      ),
    );
  }

  void _showTaskSheet(
      BuildContext context, WishPoolSnapshot snapshot, ChildTask task) {
    // The bottom-sheet builder gets a new route context.  WishPoolScope lives
    // above the page route, so resolve it before opening the sheet and capture
    // the page context for navigation/snackbars.
    final pageContext = context;
    final scope = WishPoolScope.of(pageContext);
    showModalBottomSheet<void>(
      context: pageContext,
      showDragHandle: true,
      builder: (sheetContext) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(task.title,
                style:
                    const TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
            const SizedBox(height: WishPoolSpacing.sm),
            Text(
              task.submissionTypeCode == 'manual'
                  ? '确认完成后会直接生成提交记录，等待家长确认。'
                  : '选择${task.submissionType}素材后，会创建上传会话、完成直传并生成提交记录。',
            ),
            const SizedBox(height: WishPoolSpacing.md),
            FilledButton.icon(
              onPressed: task.submissionTypeCode == 'manual'
                  ? () => _submitManualTask(pageContext, scope, snapshot, task)
                  : () => _pickAndSubmitMediaTask(
                      pageContext, scope, snapshot, task),
              icon: const Icon(Icons.cloud_upload_outlined),
              label:
                  Text(task.submissionTypeCode == 'manual' ? '提交确认' : '选择素材'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _submitManualTask(BuildContext context, WishPoolScope scope,
      WishPoolSnapshot snapshot, ChildTask task) async {
    if (!_hasRemoteContext(scope, context, snapshot)) return;
    try {
      await scope.submitManualTask(task);
      if (!context.mounted) return;
      Navigator.pop(context);
      setState(() {
        _snapshotFuture = scope.loadSnapshot();
      });
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('已提交，等待家长确认。')));
    } catch (_) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('提交失败，请稍后再试。')));
    }
  }

  Future<void> _pickAndSubmitMediaTask(BuildContext context,
      WishPoolScope scope, WishPoolSnapshot snapshot, ChildTask task) async {
    if (!_hasRemoteContext(scope, context, snapshot)) return;
    Navigator.pop(context);
    await Navigator.push<void>(
        context,
        MaterialPageRoute(
            builder: (_) => MediaCaptureScreen(
                task: task, scope: scope, onSubmitted: _reloadSnapshot)));
  }

  bool _hasRemoteContext(
      WishPoolScope scope, BuildContext context, WishPoolSnapshot snapshot) {
    if (snapshot.source == 'core-api' && scope.config.hasRemoteContext) {
      return true;
    }
    Navigator.pop(context);
    ScaffoldMessenger.of(context)
        .showSnackBar(const SnackBar(content: Text('请先完成登录或儿童配对，再提交任务。')));
    return false;
  }
}

class _UploadQueueBanner extends StatelessWidget {
  const _UploadQueueBanner({required this.queue, required this.onRetry});

  final UploadQueue queue;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    if (queue.items.isEmpty) return const SizedBox.shrink();
    final item = queue.items.first;
    final label = queue.uploadingCount > 0
        ? '正在上传…'
        : queue.failedCount > 0
            ? '失败可重试：${item.lastError ?? '请稍后重试'}'
            : '待上传 ${queue.pendingCount} 项';
    return Material(
      color: Theme.of(context).colorScheme.secondaryContainer,
      child: ListTile(
        dense: true,
        leading: const Icon(Icons.cloud_upload_outlined),
        title: Text(label),
        trailing: TextButton(onPressed: onRetry, child: const Text('立即重试')),
      ),
    );
  }
}
