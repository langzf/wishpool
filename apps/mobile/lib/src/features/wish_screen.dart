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
                const Text('完成本周核心任务后，周六下午一起搭城市小屋。'),
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
                const Text('6 / 10 心愿碎片'),
                const SizedBox(height: WishPoolSpacing.lg),
                FilledButton.icon(
                  onPressed: () {},
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
}
