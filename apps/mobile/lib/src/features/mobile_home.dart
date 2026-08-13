import 'dart:io';
import 'dart:async';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:mime/mime.dart';

import '../data/wishpool_scope.dart';
import '../design/wishpool_theme.dart';
import '../domain/wishpool_snapshot.dart';
import '../shared/fixture_data.dart';
import 'child_dashboard.dart';
import 'notification_screen.dart';
import 'parent_dashboard.dart';
import 'room_screen.dart';
import 'wish_screen.dart';

class MobileHomeScreen extends StatefulWidget {
  const MobileHomeScreen({
    super.key,
    this.childMode = false,
    this.onSignOut,
  });

  final bool childMode;
  final VoidCallback? onSignOut;

  @override
  State<MobileHomeScreen> createState() => _MobileHomeScreenState();
}

class _MobileHomeScreenState extends State<MobileHomeScreen> {
  int _index = 0;
  late Future<WishPoolSnapshot> _snapshotFuture;
  StreamSubscription? _syncSubscription;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _snapshotFuture = WishPoolScope.of(context).loadSnapshot();
    _startRealtimeSync();
  }

  @override
  void dispose() {
    _syncSubscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<WishPoolSnapshot>(
      future: _snapshotFuture,
      builder: (context, snapshot) {
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
          RoomScreen(
            snapshot: data,
            onRoomChanged: _reloadSnapshot,
            onSignOut: widget.childMode ? widget.onSignOut : null,
          ),
          if (!widget.childMode)
            ParentDashboardScreen(
              snapshot: data,
              onSignOut: widget.onSignOut,
              onDataChanged: _reloadSnapshot,
            ),
        ];
        final destinations = <NavigationDestination>[
          const NavigationDestination(icon: Icon(Icons.check_circle_outline), label: '任务'),
          const NavigationDestination(icon: Icon(Icons.favorite_border), label: '心愿'),
          const NavigationDestination(icon: Icon(Icons.notifications_none), label: '通知'),
          const NavigationDestination(icon: Icon(Icons.home_outlined), label: '小屋'),
          if (!widget.childMode) const NavigationDestination(icon: Icon(Icons.verified_outlined), label: '家长'),
        ];
        final selectedIndex = _index.clamp(0, screens.length - 1);
        return Scaffold(
          body: SafeArea(child: screens[selectedIndex]),
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

  void _startRealtimeSync() {
    if (_syncSubscription != null) return;
    final scope = WishPoolScope.of(context);
    final config = scope.config;
    if (!config.hasRemoteContext || config.accessToken == null || config.familyId == null) return;
    scope.syncCoordinator.start(familyId: config.familyId!, accessToken: config.accessToken!);
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
    final openTasks = snapshot.childTasks.where((task) => task.status != '已通过').toList();
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (context) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('选择今天的小冒险', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
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

  void _showTaskSheet(BuildContext context, WishPoolSnapshot snapshot, ChildTask task) {
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (context) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(task.title, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
            const SizedBox(height: WishPoolSpacing.sm),
            Text(
              task.submissionTypeCode == 'manual'
                  ? '确认完成后会直接生成提交记录，等待家长确认。'
                  : '选择${task.submissionType}素材后，会创建上传会话、完成直传并生成提交记录。',
            ),
            const SizedBox(height: WishPoolSpacing.md),
            FilledButton.icon(
              onPressed: task.submissionTypeCode == 'manual'
                  ? () => _submitManualTask(context, snapshot, task)
                  : () => _pickAndSubmitMediaTask(context, snapshot, task),
              icon: const Icon(Icons.cloud_upload_outlined),
              label: Text(task.submissionTypeCode == 'manual' ? '提交确认' : '选择素材'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _submitManualTask(BuildContext context, WishPoolSnapshot snapshot, ChildTask task) async {
    if (!_hasRemoteContext(context, snapshot)) return;
    try {
      await WishPoolScope.of(context).submitManualTask(task);
      if (!context.mounted) return;
      Navigator.pop(context);
      setState(() {
        _snapshotFuture = WishPoolScope.of(context).loadSnapshot();
      });
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('已提交，等待家长确认。')));
    } catch (_) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('提交失败，请稍后再试。')));
    }
  }

  Future<void> _pickAndSubmitMediaTask(BuildContext context, WishPoolSnapshot snapshot, ChildTask task) async {
    if (!_hasRemoteContext(context, snapshot)) return;
    try {
      final result = await FilePicker.platform.pickFiles(
        allowMultiple: false,
        type: _pickerType(task),
        withData: false,
      );
      final path = result?.files.single.path;
      if (path == null) return;

      final contentType = lookupMimeType(path) ?? _fallbackContentType(task);
      if (!_matchesTaskContentType(contentType, task.submissionTypeCode)) {
        if (!context.mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('请选择${task.submissionType}素材。')));
        return;
      }

      await WishPoolScope.of(context).submitMediaTask(
        task: task,
        file: File(path),
        contentType: contentType,
      );
      if (!context.mounted) return;
      Navigator.pop(context);
      setState(() {
        _snapshotFuture = WishPoolScope.of(context).loadSnapshot();
      });
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('已提交，等待家长确认。')));
    } catch (_) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('上传失败，已保留在本地队列中。')));
    }
  }

  bool _hasRemoteContext(BuildContext context, WishPoolSnapshot snapshot) {
    if (snapshot.source == 'core-api' && WishPoolScope.of(context).config.hasRemoteContext) return true;
    Navigator.pop(context);
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('请先完成登录或儿童配对，再提交任务。')));
    return false;
  }

  FileType _pickerType(ChildTask task) {
    return switch (task.submissionTypeCode) {
      'photo' => FileType.image,
      'audio' => FileType.audio,
      'video' => FileType.video,
      _ => FileType.any,
    };
  }

  String _fallbackContentType(ChildTask task) {
    return switch (task.submissionTypeCode) {
      'photo' => 'image/jpeg',
      'audio' => 'audio/mpeg',
      'video' => 'video/mp4',
      _ => 'application/octet-stream',
    };
  }

  bool _matchesTaskContentType(String contentType, String submissionTypeCode) {
    return switch (submissionTypeCode) {
      'photo' => contentType.startsWith('image/'),
      'audio' => contentType.startsWith('audio/'),
      'video' => contentType.startsWith('video/'),
      'manual' => true,
      _ => false,
    };
  }
}
