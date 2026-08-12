import 'package:flutter/material.dart';

import '../design/wishpool_theme.dart';
import '../domain/wishpool_snapshot.dart';
import '../shared/fixture_data.dart';

class ChildDashboardScreen extends StatelessWidget {
  const ChildDashboardScreen({
    super.key,
    required this.snapshot,
  });

  final WishPoolSnapshot snapshot;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return CustomScrollView(
      slivers: [
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(16, 20, 16, 12),
          sliver: SliverToBoxAdapter(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('今天好，${snapshot.childName}', style: theme.textTheme.bodyLarge),
                const SizedBox(height: WishPoolSpacing.xs),
                Text('把今天的小星光收进口袋', style: theme.textTheme.headlineLarge),
                const SizedBox(height: WishPoolSpacing.md),
                const _TodaySummaryCard(),
              ],
            ),
          ),
        ),
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 120),
          sliver: SliverList.separated(
            itemCount: snapshot.childTasks.length,
            separatorBuilder: (_, __) => const SizedBox(height: WishPoolSpacing.sm),
            itemBuilder: (context, index) => _TaskCard(task: snapshot.childTasks[index]),
          ),
        ),
      ],
    );
  }
}

class _TodaySummaryCard extends StatelessWidget {
  const _TodaySummaryCard();

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

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
            const Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('今日 3 个任务'),
                  SizedBox(height: 4),
                  Text('已完成 1 个，2 个正在路上'),
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
  const _TaskCard({required this.task});

  final ChildTask task;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(8),
        onTap: () {},
        child: Padding(
          padding: const EdgeInsets.all(WishPoolSpacing.md),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(child: Text(task.title, style: theme.textTheme.titleLarge)),
                  _StatusChip(label: task.status),
                ],
              ),
              const SizedBox(height: WishPoolSpacing.sm),
              Row(
                children: [
                  Icon(Icons.category_outlined, size: 18, color: theme.colorScheme.secondary),
                  const SizedBox(width: WishPoolSpacing.xs),
                  Expanded(child: Text('${task.category} · ${task.submissionType}')),
                  Text('+${task.reward} 星光', style: theme.textTheme.labelLarge),
                ],
              ),
            ],
          ),
        ),
      ),
    );
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
        child: Text(label, style: TextStyle(color: color, fontWeight: FontWeight.w800)),
      ),
    );
  }
}
