import 'package:flutter/material.dart';

import '../data/wishpool_scope.dart';
import '../design/wishpool_theme.dart';
import '../domain/wishpool_snapshot.dart';
import '../shared/fixture_data.dart';

class NotificationScreen extends StatelessWidget {
  const NotificationScreen({
    super.key,
    required this.snapshot,
    this.showPreferences = false,
    this.onChanged,
  });

  final WishPoolSnapshot snapshot;
  final bool showPreferences;
  final VoidCallback? onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 120),
      children: [
        Text('通知中心', style: theme.textTheme.headlineLarge),
        const SizedBox(height: WishPoolSpacing.xs),
        Text('${snapshot.unreadNotifications} 条未读提醒', style: theme.textTheme.bodyMedium),
        const SizedBox(height: WishPoolSpacing.md),
        ...snapshot.notificationInbox.map(
          (item) => Padding(
            padding: const EdgeInsets.only(bottom: WishPoolSpacing.sm),
            child: _NotificationCard(
              item: item,
              onChanged: onChanged,
            ),
          ),
        ),
        if (showPreferences) ...[
          const SizedBox(height: WishPoolSpacing.md),
          Text('通知偏好', style: theme.textTheme.titleLarge),
          const SizedBox(height: WishPoolSpacing.sm),
          ...snapshot.notificationPreferences.map(
            (preference) => Padding(
              padding: const EdgeInsets.only(bottom: WishPoolSpacing.sm),
              child: _PreferenceTile(
                preference: preference,
                onChanged: onChanged,
              ),
            ),
          ),
        ],
      ],
    );
  }
}

class _NotificationCard extends StatelessWidget {
  const _NotificationCard({
    required this.item,
    this.onChanged,
  });

  final NotificationItemData item;
  final VoidCallback? onChanged;

  @override
  Widget build(BuildContext context) {
    final isRead = item.status == 'read';
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(WishPoolSpacing.md),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(isRead ? Icons.mark_email_read_outlined : Icons.notifications_active_outlined),
                const SizedBox(width: WishPoolSpacing.sm),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(item.title, style: Theme.of(context).textTheme.titleLarge),
                      const SizedBox(height: 4),
                      Text(item.body),
                      const SizedBox(height: 4),
                      Text(item.createdAt, style: Theme.of(context).textTheme.bodySmall),
                    ],
                  ),
                ),
              ],
            ),
            if (!isRead) ...[
              const SizedBox(height: WishPoolSpacing.sm),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton.icon(
                  onPressed: () => _markRead(context),
                  icon: const Icon(Icons.done_all_outlined),
                  label: const Text('标记已读'),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Future<void> _markRead(BuildContext context) async {
    await WishPoolScope.of(context).markNotificationsRead([item.id]);
    if (!context.mounted) return;
    onChanged?.call();
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('已标记为已读')));
  }
}

class _PreferenceTile extends StatelessWidget {
  const _PreferenceTile({
    required this.preference,
    this.onChanged,
  });

  final NotificationPreferenceData preference;
  final VoidCallback? onChanged;

  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      value: preference.enabled,
      title: Text(_label(preference.type)),
      subtitle: const Text('站内通知开启，系统推送由本地部署配置决定'),
      onChanged: (enabled) => _update(context, enabled),
    );
  }

  Future<void> _update(BuildContext context, bool enabled) async {
    await WishPoolScope.of(context).updateNotificationPreference(preference, enabled: enabled);
    if (!context.mounted) return;
    onChanged?.call();
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('通知偏好已更新')));
  }

  String _label(String type) {
    return switch (type) {
      'child_submission_created' => '孩子提交提醒',
      'review_completed' => '审核反馈提醒',
      'wish_unlocked' => '心愿解锁提醒',
      'task_plan_changed' => '任务变更提醒',
      'ai_precheck_completed' => 'AI 预审提醒',
      'wish_fragment_earned' => '心愿碎片提醒',
      'wish_redeemed_memory_generated' => '纪念册生成提醒',
      _ => '家庭提醒',
    };
  }
}
