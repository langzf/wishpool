import 'package:flutter/material.dart';

import '../data/wishpool_scope.dart';
import '../design/wishpool_theme.dart';
import '../domain/wishpool_snapshot.dart';
import '../shared/fixture_data.dart';

class ParentDashboardScreen extends StatelessWidget {
  const ParentDashboardScreen({
    super.key,
    required this.snapshot,
    this.onSignOut,
    this.onDataChanged,
  });

  final WishPoolSnapshot snapshot;
  final VoidCallback? onSignOut;
  final VoidCallback? onDataChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 120),
      children: [
        Text('家长审核', style: theme.textTheme.headlineLarge),
        const SizedBox(height: WishPoolSpacing.md),
        if (snapshot.reviewCards.isEmpty)
          const _InfoCard(
            icon: Icons.verified_outlined,
            title: '没有待审核提交',
            body: '孩子完成任务后，提交会进入这里并同步提醒你处理。',
          )
        else
          ...snapshot.reviewCards.map((card) => Padding(
                padding: const EdgeInsets.only(bottom: WishPoolSpacing.sm),
                child: _ReviewCard(
                  card: card,
                  onReviewed: onDataChanged,
                ),
              )),
        const SizedBox(height: WishPoolSpacing.md),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(WishPoolSpacing.md),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('今日任务调整', style: theme.textTheme.titleLarge),
                const SizedBox(height: WishPoolSpacing.sm),
                if (snapshot.childTasks.isEmpty)
                  const Text('今天没有已生成任务，保存周计划后会生成当天任务。')
                else
                  for (final task in snapshot.childTasks)
                    _TaskAdjustRow(
                      task: task,
                      onChanged: onDataChanged,
                    ),
              ],
            ),
          ),
        ),
        const SizedBox(height: WishPoolSpacing.md),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(WishPoolSpacing.md),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('本周计划', style: theme.textTheme.titleLarge),
                const SizedBox(height: WishPoolSpacing.xs),
                if (snapshot.weeklyPlanRules.isEmpty)
                  const Text('还没有本周计划。在家长 Web 保存计划后，任务会自动同步到孩子端。')
                else
                  for (final rule in snapshot.weeklyPlanRules)
                    Padding(
                      padding: const EdgeInsets.only(top: WishPoolSpacing.xs),
                      child: Row(
                        children: [
                          Icon(rule.isCore ? Icons.flag_outlined : Icons.lightbulb_outline, size: 18),
                          const SizedBox(width: WishPoolSpacing.xs),
                          Expanded(child: Text(rule.title)),
                          Text('${rule.weekdays.length} 天'),
                        ],
                      ),
                    ),
                const SizedBox(height: WishPoolSpacing.md),
                OutlinedButton.icon(
                  onPressed: () => _showPlanSheet(context),
                  icon: const Icon(Icons.edit_calendar_outlined),
                  label: const Text('查看计划说明'),
                ),
              ],
            ),
          ),
        ),
        if (onSignOut != null) ...[
          const SizedBox(height: WishPoolSpacing.md),
          OutlinedButton.icon(
            onPressed: onSignOut,
            icon: const Icon(Icons.logout_outlined),
            label: const Text('退出登录'),
          ),
        ],
      ],
    );
  }

  void _showPlanSheet(BuildContext context) {
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (context) => const Padding(
        padding: EdgeInsets.fromLTRB(20, 8, 20, 28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('本周计划', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
            SizedBox(height: 12),
            Text('本周计划由任务模板保存并生成每日任务。手机端可处理当天跳过、延后和审核，批量编辑在家长 Web 完成。'),
          ],
        ),
      ),
    );
  }
}

class _InfoCard extends StatelessWidget {
  const _InfoCard({
    required this.icon,
    required this.title,
    required this.body,
  });

  final IconData icon;
  final String title;
  final String body;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(WishPoolSpacing.md),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: WishPoolColors.primary),
            const SizedBox(width: WishPoolSpacing.sm),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: Theme.of(context).textTheme.titleLarge),
                  const SizedBox(height: 4),
                  Text(body),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _TaskAdjustRow extends StatelessWidget {
  const _TaskAdjustRow({
    required this.task,
    this.onChanged,
  });

  final ChildTask task;
  final VoidCallback? onChanged;

  @override
  Widget build(BuildContext context) {
    final adjustable = task.status == '待打卡' || task.status == '需修改';
    return Padding(
      padding: const EdgeInsets.only(top: WishPoolSpacing.sm),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(task.title, style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 2),
                Text('${task.submissionType}提交 · ${task.status}', style: Theme.of(context).textTheme.bodySmall),
              ],
            ),
          ),
          IconButton(
            tooltip: '跳过任务',
            onPressed: adjustable ? () => _skip(context) : null,
            icon: const Icon(Icons.skip_next_outlined),
          ),
          IconButton(
            tooltip: '延后任务',
            onPressed: adjustable ? () => _postpone(context) : null,
            icon: const Icon(Icons.update_outlined),
          ),
        ],
      ),
    );
  }

  Future<void> _skip(BuildContext context) async {
    try {
      await WishPoolScope.of(context).skipTask(task);
      if (!context.mounted) return;
      onChanged?.call();
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('已跳过今天的任务')));
    } catch (_) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('任务调整失败，请稍后再试')));
    }
  }

  Future<void> _postpone(BuildContext context) async {
    try {
      await WishPoolScope.of(context).postponeTask(task);
      if (!context.mounted) return;
      onChanged?.call();
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('已顺延到下一天')));
    } catch (_) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('任务调整失败，请稍后再试')));
    }
  }
}

class _ReviewCard extends StatelessWidget {
  const _ReviewCard({
    required this.card,
    this.onReviewed,
  });

  final ReviewCardData card;
  final VoidCallback? onReviewed;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(WishPoolSpacing.md),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(_iconFor(card.mediaType), color: WishPoolColors.primary),
                const SizedBox(width: WishPoolSpacing.xs),
                Expanded(child: Text(card.title, style: theme.textTheme.titleLarge)),
              ],
            ),
            const SizedBox(height: WishPoolSpacing.sm),
            Text(card.summary),
            const SizedBox(height: WishPoolSpacing.md),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () => _requestRevision(context),
                    icon: const Icon(Icons.rate_review_outlined),
                    label: const Text('退回'),
                  ),
                ),
                const SizedBox(width: WishPoolSpacing.sm),
                Expanded(
                  child: FilledButton.icon(
                    onPressed: () => _approve(context),
                    icon: const Icon(Icons.check_circle_outline),
                    label: const Text('通过'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  IconData _iconFor(String mediaType) {
    return switch (mediaType) {
      '语音' => Icons.mic_none,
      '视频' => Icons.videocam_outlined,
      _ => Icons.image_outlined,
    };
  }

  Future<void> _approve(BuildContext context) async {
    await WishPoolScope.of(context).approveReview(card.submissionId);
    if (!context.mounted) return;
    onReviewed?.call();
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('已通过，反馈会同步给孩子')));
  }

  Future<void> _requestRevision(BuildContext context) async {
    await WishPoolScope.of(context).requestRevision(card.submissionId);
    if (!context.mounted) return;
    onReviewed?.call();
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('已退回修改，孩子端会收到提示')));
  }
}
