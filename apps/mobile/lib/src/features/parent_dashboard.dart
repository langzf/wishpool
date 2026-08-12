import 'package:flutter/material.dart';

import '../design/wishpool_theme.dart';
import '../domain/wishpool_snapshot.dart';
import '../shared/fixture_data.dart';

class ParentDashboardScreen extends StatelessWidget {
  const ParentDashboardScreen({
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
        Text('家长审核', style: theme.textTheme.headlineLarge),
        const SizedBox(height: WishPoolSpacing.md),
        ...snapshot.reviewCards.map((card) => Padding(
              padding: const EdgeInsets.only(bottom: WishPoolSpacing.sm),
              child: _ReviewCard(card: card),
            )),
        const SizedBox(height: WishPoolSpacing.md),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(WishPoolSpacing.md),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('本周计划', style: theme.textTheme.titleLarge),
                const SizedBox(height: WishPoolSpacing.xs),
                const Text('阅读 5 天 · 钢琴 3 天 · 运动 3 天'),
                const SizedBox(height: WishPoolSpacing.md),
                OutlinedButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.edit_calendar_outlined),
                  label: const Text('调整计划'),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _ReviewCard extends StatelessWidget {
  const _ReviewCard({required this.card});

  final ReviewCardData card;

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
                    onPressed: () {},
                    icon: const Icon(Icons.rate_review_outlined),
                    label: const Text('退回'),
                  ),
                ),
                const SizedBox(width: WishPoolSpacing.sm),
                Expanded(
                  child: FilledButton.icon(
                    onPressed: () {},
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
}
