import 'package:flutter/material.dart';

import '../design/wishpool_theme.dart';
import '../domain/wishpool_snapshot.dart';
import '../shared/fixture_data.dart';

String childStarLightLabel(int amount) =>
    amount > 0 ? '+$amount 星光' : '星光以账本发放为准';

class ChildDashboardScreen extends StatelessWidget {
  const ChildDashboardScreen({
    super.key,
    required this.snapshot,
    required this.onStartTask,
  });

  final WishPoolSnapshot snapshot;
  final ValueChanged<ChildTask> onStartTask;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final openTasks = snapshot.childTasks;

    return CustomScrollView(
      slivers: [
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(16, 20, 16, 12),
          sliver: SliverToBoxAdapter(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('今天好，${snapshot.childName}',
                    style: theme.textTheme.bodyLarge),
                const SizedBox(height: WishPoolSpacing.xs),
                Text('把今天的小星光收进口袋', style: theme.textTheme.headlineLarge),
                if (snapshot.unreadNotifications > 0) ...[
                  const SizedBox(height: WishPoolSpacing.xs),
                  Text('${snapshot.unreadNotifications} 条新提醒在通知中心',
                      style: theme.textTheme.bodyMedium),
                ],
                const SizedBox(height: WishPoolSpacing.md),
                _TodaySummaryCard(tasks: snapshot.childTasks),
              ],
            ),
          ),
        ),
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 120),
          sliver: openTasks.isEmpty
              ? const SliverToBoxAdapter(
                  child: _EmptyStateCard(
                    icon: Icons.check_circle_outline,
                    title: '今天没有任务',
                    body: '已经完成的任务会等待家长确认，新的安排会同步到这里。',
                  ),
                )
              : SliverList.separated(
                  itemCount: openTasks.length,
                  separatorBuilder: (_, __) =>
                      const SizedBox(height: WishPoolSpacing.sm),
                  itemBuilder: (context, index) => _TaskCard(
                    task: openTasks[index],
                    onStartTask: onStartTask,
                  ),
                ),
        ),
      ],
    );
  }
}

class _EmptyStateCard extends StatelessWidget {
  const _EmptyStateCard({
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

class _TodaySummaryCard extends StatelessWidget {
  const _TodaySummaryCard({required this.tasks});

  final List<ChildTask> tasks;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final completed = tasks.where((task) => task.status == '已通过').length;
    final waiting = tasks.where((task) => task.status == '待审核').length;
    final open = tasks.length - completed - waiting;
    final headline = tasks.isEmpty ? '今日没有任务' : '今日 ${tasks.length} 个任务';
    final detail = tasks.isEmpty
        ? '家长安排新任务后会出现在这里。'
        : '已完成 $completed 个，待审核 $waiting 个，待打卡 $open 个。';

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(WishPoolSpacing.md),
        child: Row(
          children: [
            Container(
              alignment: Alignment.center,
              height: 56,
              width: 56,
              decoration: BoxDecoration(
                color: WishPoolColors.muted,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(Icons.auto_awesome, color: colorScheme.primary),
            ),
            const SizedBox(width: WishPoolSpacing.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(headline),
                  const SizedBox(height: 4),
                  Text(detail),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _TaskCard extends StatelessWidget {
  const _TaskCard({
    required this.task,
    required this.onStartTask,
  });

  final ChildTask task;
  final ValueChanged<ChildTask> onStartTask;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(8),
        onTap: () => _showTaskDetail(context),
        child: Padding(
          padding: const EdgeInsets.all(WishPoolSpacing.md),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                      child:
                          Text(task.title, style: theme.textTheme.titleLarge)),
                  _StatusChip(label: task.status),
                ],
              ),
              const SizedBox(height: WishPoolSpacing.sm),
              Text(_statusExplanation(task), style: theme.textTheme.bodyMedium),
              if (task.feedback != null && task.feedback!.isNotEmpty) ...[
                const SizedBox(height: 6),
                Text('家长反馈：${task.feedback}',
                    style: TextStyle(color: theme.colorScheme.error)),
              ],
              const SizedBox(height: WishPoolSpacing.sm),
              Row(
                children: [
                  Icon(Icons.category_outlined,
                      size: 18, color: theme.colorScheme.secondary),
                  const SizedBox(width: WishPoolSpacing.xs),
                  Expanded(
                      child: Text('${task.category} · ${task.submissionType}')),
                  Text(childStarLightLabel(task.reward),
                      style: theme.textTheme.labelLarge),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showTaskDetail(BuildContext context) {
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (context) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(task.title,
                style:
                    const TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
            const SizedBox(height: WishPoolSpacing.sm),
            Text(task.reward > 0
                ? '${task.category}任务 · ${task.submissionType}提交 · 完成后获得 ${task.reward} 星光'
                : '${task.category}任务 · ${task.submissionType}提交 · 星光以账本实际发放为准'),
            const SizedBox(height: WishPoolSpacing.md),
            Text(_statusExplanation(task)),
            if (task.feedback != null && task.feedback!.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text('家长反馈：${task.feedback}',
                  style: TextStyle(color: Theme.of(context).colorScheme.error)),
            ],
            if (task.status != '已通过' &&
                task.status != '待家长审核' &&
                task.status != '已提交·AI 预审中') ...[
              const SizedBox(height: WishPoolSpacing.md),
              FilledButton.icon(
                onPressed: () {
                  Navigator.pop(context);
                  onStartTask(task);
                },
                icon: Icon(task.status == '被退回' || task.status == '已过期/未完成'
                    ? Icons.refresh
                    : Icons.play_circle_outline),
                label: Text(task.status == '被退回' || task.status == '已过期/未完成'
                    ? '补做'
                    : '开始打卡'),
              ),
            ],
          ],
        ),
      ),
    );
  }

  String _statusExplanation(ChildTask task) {
    return switch (task.status) {
      '待打卡' => '今天还没做，快去打卡吧',
      '已提交·AI 预审中' => '已提交，正在智能检查…',
      '待家长审核' => '已提交，等家长看看',
      '已通过' => task.reward > 0 ? '已完成，获得 ${task.reward} 星光' : '已完成，星光以账本实际发放为准',
      '被退回' => '被退回了：${task.feedback ?? '请查看家长反馈'}，下一步：补做',
      '已过期/未完成' => '这项任务已过期或未完成，可以补做',
      _ => '下一步：完成这项任务并提交',
    };
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    final color = switch (label) {
      '已通过' => WishPoolColors.secondary,
      '待审核' => WishPoolColors.accent,
      _ => WishPoolColors.primary,
    };

    return Semantics(
      label: '任务状态 $label',
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.12),
          borderRadius: BorderRadius.circular(999),
        ),
        child: Text(label,
            style: TextStyle(color: color, fontWeight: FontWeight.w800)),
      ),
    );
  }
}
