import 'package:flutter/material.dart';

import '../design/wishpool_theme.dart';
import '../domain/wishpool_snapshot.dart';

class WishScreen extends StatelessWidget {
  const WishScreen({
    super.key,
    required this.snapshot,
  });

  final WishPoolSnapshot snapshot;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 120),
      children: [
        Text('心愿卡', style: theme.textTheme.headlineLarge),
        const SizedBox(height: WishPoolSpacing.md),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(WishPoolSpacing.lg),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  height: 180,
                  decoration: BoxDecoration(
                    color: WishPoolColors.muted,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Center(
                    child: Icon(Icons.extension_outlined, size: 64, color: theme.colorScheme.primary),
                  ),
                ),
                const SizedBox(height: WishPoolSpacing.md),
                Text(snapshot.wishTitle, style: theme.textTheme.headlineMedium),
                const SizedBox(height: WishPoolSpacing.xs),
                Text(
                  snapshot.wishTargetFragments <= 1 && snapshot.wishCurrentFragments == 0
                      ? '家长创建本周心愿后，核心任务通过审核会推动碎片进度。'
                      : '完成本周核心任务并通过家长确认后，会获得心愿碎片。',
                ),
                const SizedBox(height: WishPoolSpacing.lg),
                ClipRRect(
                  borderRadius: BorderRadius.circular(999),
                  child: LinearProgressIndicator(
                    value: snapshot.wishProgress,
                    minHeight: 12,
                    backgroundColor: WishPoolColors.border,
                    color: WishPoolColors.secondary,
                  ),
                ),
                const SizedBox(height: WishPoolSpacing.sm),
                Text('${snapshot.wishCurrentFragments} / ${snapshot.wishTargetFragments} 心愿碎片'),
                const SizedBox(height: WishPoolSpacing.lg),
                FilledButton.icon(
                  onPressed: () => _showWishRule(context),
                  icon: const Icon(Icons.card_giftcard),
                  label: const Text('查看兑现规则'),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  void _showWishRule(BuildContext context) {
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (context) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(snapshot.wishTitle, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
            const SizedBox(height: WishPoolSpacing.sm),
            Text('当前进度 ${(snapshot.wishProgress * 100).round()}%。当天核心任务完成并经家长确认后，会获得一块心愿碎片。'),
          ],
        ),
      ),
    );
  }
}
