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
    final remaining = (snapshot.wishTargetFragments - snapshot.wishCurrentFragments).clamp(0, snapshot.wishTargetFragments).toInt();
    final hasWish = snapshot.wishTargetFragments > 1 || snapshot.wishCurrentFragments > 0;

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 120),
      children: [
        Text('心愿卡', style: theme.textTheme.bodyLarge),
        const SizedBox(height: WishPoolSpacing.xs),
        Text(hasWish ? snapshot.wishTitle : '等待新的心愿', style: theme.textTheme.headlineLarge),
        const SizedBox(height: WishPoolSpacing.md),
        _WishHeroCard(
          title: snapshot.wishTitle,
          progress: snapshot.wishProgress,
          currentFragments: snapshot.wishCurrentFragments,
          targetFragments: snapshot.wishTargetFragments,
          imageUrl: snapshot.wishImageUrl,
          fragmentMode: snapshot.wishFragmentVisualMode,
          fragmentRows: snapshot.wishFragmentRows,
          fragmentCols: snapshot.wishFragmentCols,
          fragmentMask: snapshot.wishFragmentMask,
          litIndexes: snapshot.wishLitIndexes,
          remainingFragments: remaining,
          hasWish: hasWish,
          onRuleTap: () => _showWishRule(context, remaining),
        ),
        const SizedBox(height: WishPoolSpacing.md),
        _EncourageCard(
          remainingFragments: remaining,
          latestMemoryTitle: snapshot.latestMemoryTitle,
          hasWish: hasWish,
        ),
      ],
    );
  }

  void _showWishRule(BuildContext context, int remaining) {
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
            Text(
              remaining == 0
                  ? '碎片已经全部点亮，可以请家长安排兑现。'
                  : '完成核心任务并通过家长审核，就能点亮一块碎片。还差 $remaining 块。',
            ),
            const SizedBox(height: WishPoolSpacing.md),
            _FragmentBoard(
              current: snapshot.wishCurrentFragments,
              target: snapshot.wishTargetFragments,
              imageUrl: snapshot.wishImageUrl,
              mode: snapshot.wishFragmentVisualMode,
              rows: snapshot.wishFragmentRows,
              cols: snapshot.wishFragmentCols,
              mask: snapshot.wishFragmentMask,
              litIndexes: snapshot.wishLitIndexes,
              compact: true,
            ),
          ],
        ),
      ),
    );
  }
}

class _WishHeroCard extends StatelessWidget {
  const _WishHeroCard({
    required this.title,
    required this.progress,
    required this.currentFragments,
    required this.targetFragments,
    required this.imageUrl,
    required this.fragmentMode,
    required this.fragmentRows,
    required this.fragmentCols,
    required this.fragmentMask,
    required this.litIndexes,
    required this.remainingFragments,
    required this.hasWish,
    required this.onRuleTap,
  });

  final String title;
  final double progress;
  final int currentFragments;
  final int targetFragments;
  final String? imageUrl;
  final String fragmentMode;
  final int? fragmentRows;
  final int? fragmentCols;
  final WishFragmentMaskData? fragmentMask;
  final List<int> litIndexes;
  final int remainingFragments;
  final bool hasWish;
  final VoidCallback onRuleTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      color: theme.colorScheme.surface,
      child: Padding(
        padding: const EdgeInsets.all(WishPoolSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _FragmentBoard(
              current: currentFragments,
              target: targetFragments,
              imageUrl: imageUrl,
              mode: fragmentMode,
              rows: fragmentRows,
              cols: fragmentCols,
              mask: fragmentMask,
              litIndexes: litIndexes,
            ),
            const SizedBox(height: WishPoolSpacing.lg),
            Text(title, style: theme.textTheme.headlineMedium),
            const SizedBox(height: WishPoolSpacing.xs),
            Text(hasWish ? _progressText() : '新的心愿会在这里亮起来。'),
            const SizedBox(height: WishPoolSpacing.md),
            Row(
              children: [
                Expanded(
                  child: Text(
                    '$currentFragments / $targetFragments 块碎片',
                    style: theme.textTheme.labelLarge?.copyWith(color: WishPoolColors.secondary),
                  ),
                ),
                FilledButton.icon(
                  onPressed: onRuleTap,
                  icon: const Icon(Icons.info_outline),
                  label: const Text('规则'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _progressText() {
    if (remainingFragments == 0) return '已集满，可以兑现心愿。';
    if (currentFragments == 0) return '完成一个核心任务，点亮第一块碎片。';
    return '已点亮 ${(progress * 100).round()}%，还差 $remainingFragments 块。';
  }
}

class _FragmentBoard extends StatelessWidget {
  const _FragmentBoard({
    required this.current,
    required this.target,
    this.imageUrl,
    this.mode = 'grid_reveal',
    this.rows,
    this.cols,
    this.mask,
    this.litIndexes = const <int>[],
    this.compact = false,
  });

  final int current;
  final int target;
  final String? imageUrl;
  final String mode;
  final int? rows;
  final int? cols;
  final WishFragmentMaskData? mask;
  final List<int> litIndexes;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final layout = _fragmentLayout(target, mode: mode, rows: rows, cols: cols, mask: mask);
    final cells = layout.rows * layout.cols;
    final filled = current.clamp(0, cells).toInt();
    final litSet = litIndexes.where((index) => index >= 0 && index < cells).toSet();
    final complete = filled == cells && target > 0;

    return Semantics(
      label: '心愿碎片 $current / $target',
      child: AspectRatio(
        aspectRatio: compact ? layout.cols / layout.rows : 1.35,
        child: ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: Stack(
            fit: StackFit.expand,
            children: [
              _WishImageBackdrop(imageUrl: imageUrl),
              GridView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                padding: EdgeInsets.zero,
                itemCount: cells,
                gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: layout.cols,
                  crossAxisSpacing: layout.mode == 'irregular' ? 2 : WishPoolSpacing.xs,
                  mainAxisSpacing: layout.mode == 'irregular' ? 2 : WishPoolSpacing.xs,
                ),
                itemBuilder: (context, index) {
                  final cell = layout.cells[index];
                  return _FragmentTile(
                    index: cell.index,
                    lit: litSet.isNotEmpty ? litSet.contains(cell.index) : cell.index < filled,
                    mode: layout.mode,
                    hasImage: imageUrl != null && imageUrl!.isNotEmpty,
                    polygon: cell.polygon,
                  );
                },
              ),
              if (complete) const _CompletionSparkle(),
            ],
          ),
        ),
      ),
    );
  }
}

class _WishImageBackdrop extends StatelessWidget {
  const _WishImageBackdrop({required this.imageUrl});

  final String? imageUrl;

  @override
  Widget build(BuildContext context) {
    if (imageUrl == null || imageUrl!.isEmpty) {
      return Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFFDBEAFE), Color(0xFFDCFCE7), Color(0xFFFEF3C7)],
          ),
        ),
        child: Stack(
          children: [
            Positioned(left: 18, top: 16, child: Icon(Icons.favorite, color: WishPoolColors.primary.withValues(alpha: 0.24), size: 72)),
            Positioned(right: 18, bottom: 16, child: Icon(Icons.auto_awesome, color: WishPoolColors.accent.withValues(alpha: 0.42), size: 82)),
          ],
        ),
      );
    }
    return Image.network(
      imageUrl!,
      fit: BoxFit.cover,
      errorBuilder: (context, error, stackTrace) => const _WishImageBackdrop(imageUrl: null),
    );
  }
}

class _FragmentTile extends StatelessWidget {
  const _FragmentTile({
    required this.index,
    required this.lit,
    required this.mode,
    required this.hasImage,
    required this.polygon,
  });

  final int index;
  final bool lit;
  final String mode;
  final bool hasImage;
  final List<WishFragmentPointData>? polygon;

  @override
  Widget build(BuildContext context) {
    final tile = AnimatedContainer(
      duration: const Duration(milliseconds: 260),
      curve: Curves.easeOutBack,
      margin: EdgeInsets.all(mode == 'irregular' ? 0 : 1),
      decoration: BoxDecoration(
        color: lit ? (hasImage ? Colors.transparent : WishPoolColors.secondary.withValues(alpha: 0.72)) : const Color(0xFF172033).withValues(alpha: 0.72),
        border: Border.all(color: lit ? Colors.white.withValues(alpha: 0.76) : Colors.white.withValues(alpha: 0.18), width: lit ? 1.5 : 1),
        borderRadius: mode == 'grid_reveal' ? BorderRadius.circular(8) : BorderRadius.circular(18),
        boxShadow: lit
            ? [
                BoxShadow(
                  color: WishPoolColors.secondary.withValues(alpha: 0.26),
                  blurRadius: 14,
                  spreadRadius: 1,
                ),
              ]
            : null,
      ),
      child: lit ? const SizedBox.shrink() : Icon(Icons.auto_awesome, color: Colors.white.withValues(alpha: 0.74), size: 18),
    );

    if (mode == 'irregular') return ClipPath(clipper: _ShardClipper(index, polygon: polygon), child: tile);
    if (mode == 'puzzle_lines') return CustomPaint(foregroundPainter: _PuzzleLinePainter(index: index, lit: lit), child: tile);
    return tile;
  }
}

class _CompletionSparkle extends StatelessWidget {
  const _CompletionSparkle();

  @override
  Widget build(BuildContext context) {
    return IgnorePointer(
      child: DecoratedBox(
        decoration: BoxDecoration(
          gradient: RadialGradient(colors: [Colors.white.withValues(alpha: 0.38), Colors.transparent]),
        ),
        child: Center(child: Icon(Icons.celebration, color: Colors.white.withValues(alpha: 0.86), size: 42)),
      ),
    );
  }
}

class _PuzzleLinePainter extends CustomPainter {
  const _PuzzleLinePainter({required this.index, required this.lit});

  final int index;
  final bool lit;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = lit ? 2 : 1.2
      ..color = (lit ? Colors.white : const Color(0xFF93C5FD)).withValues(alpha: lit ? 0.9 : 0.5);
    final path = Path()
      ..moveTo(0, 0)
      ..lineTo(size.width * 0.42, 0)
      ..arcToPoint(Offset(size.width * 0.58, 0), radius: Radius.circular(size.width * 0.12), clockwise: index.isEven)
      ..lineTo(size.width, 0)
      ..lineTo(size.width, size.height * 0.42)
      ..arcToPoint(Offset(size.width, size.height * 0.58), radius: Radius.circular(size.width * 0.12), clockwise: !index.isEven)
      ..lineTo(size.width, size.height)
      ..lineTo(0, size.height)
      ..close();
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant _PuzzleLinePainter oldDelegate) => oldDelegate.lit != lit || oldDelegate.index != index;
}

class _ShardClipper extends CustomClipper<Path> {
  const _ShardClipper(this.index, {this.polygon});

  final int index;
  final List<WishFragmentPointData>? polygon;

  @override
  Path getClip(Size size) {
    final points = polygon?.map((point) => Offset(point.x, point.y)).toList() ?? switch (index % 5) {
      0 => [const Offset(0.02, 0.08), const Offset(0.88, 0), const Offset(1, 0.72), const Offset(0.18, 1)],
      1 => [const Offset(0.12, 0), const Offset(1, 0.14), const Offset(0.86, 1), const Offset(0, 0.84)],
      2 => [const Offset(0, 0), const Offset(0.78, 0.1), const Offset(1, 1), const Offset(0.2, 0.88)],
      3 => [const Offset(0.18, 0.06), const Offset(1, 0), const Offset(0.82, 0.92), const Offset(0, 1)],
      _ => [const Offset(0, 0.2), const Offset(0.72, 0), const Offset(1, 0.8), const Offset(0.24, 1)],
    };
    final path = Path()..moveTo(points.first.dx * size.width, points.first.dy * size.height);
    for (final point in points.skip(1)) {
      path.lineTo(point.dx * size.width, point.dy * size.height);
    }
    return path..close();
  }

  @override
  bool shouldReclip(covariant _ShardClipper oldClipper) => oldClipper.index != index || oldClipper.polygon != polygon;
}

class _EncourageCard extends StatelessWidget {
  const _EncourageCard({
    required this.remainingFragments,
    required this.latestMemoryTitle,
    required this.hasWish,
  });

  final int remainingFragments;
  final String? latestMemoryTitle;
  final bool hasWish;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final title = !hasWish
        ? '新的心愿'
        : remainingFragments == 0
            ? '可以兑现'
            : '今天点亮一块';
    final body = latestMemoryTitle == null
        ? '完成任务、等待家长审核，就能看见图片一点点揭开。'
        : '最新回忆：$latestMemoryTitle';

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(WishPoolSpacing.md),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: WishPoolColors.muted,
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Icon(Icons.celebration_outlined, color: WishPoolColors.accent),
            ),
            const SizedBox(width: WishPoolSpacing.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: theme.textTheme.titleLarge),
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

({int rows, int cols, String mode, List<WishFragmentCellData> cells}) _fragmentLayout(
  int target, {
  required String mode,
  int? rows,
  int? cols,
  WishFragmentMaskData? mask,
}) {
  if (mask != null && mask.rows > 0 && mask.cols > 0 && mask.rows * mask.cols == mask.total && mask.cells.length == mask.total) {
    return (rows: mask.rows, cols: mask.cols, mode: mask.mode, cells: mask.cells);
  }
  final normalizedTarget = target > 0 ? target : 1;
  if (rows != null && cols != null && rows > 0 && cols > 0 && rows * cols == normalizedTarget) {
    return (rows: rows, cols: cols, mode: mode, cells: _defaultCells(rows, cols));
  }
  var bestRows = 1;
  var bestCols = normalizedTarget;
  var candidateRows = 1;
  while (candidateRows * candidateRows <= normalizedTarget) {
    if (normalizedTarget % candidateRows == 0) {
      bestRows = candidateRows;
      bestCols = normalizedTarget ~/ candidateRows;
    }
    candidateRows += 1;
  }
  return (rows: bestRows, cols: bestCols, mode: mode, cells: _defaultCells(bestRows, bestCols));
}

List<WishFragmentCellData> _defaultCells(int rows, int cols) {
  return List.generate(rows * cols, (index) {
    return WishFragmentCellData(index: index, row: index ~/ cols, col: index % cols);
  });
}
