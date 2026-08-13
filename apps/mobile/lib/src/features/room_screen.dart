import 'package:flutter/material.dart';

import '../data/wishpool_scope.dart';
import '../design/wishpool_theme.dart';
import '../domain/wishpool_snapshot.dart';
import '../shared/fixture_data.dart';

class RoomScreen extends StatelessWidget {
  const RoomScreen({
    super.key,
    required this.snapshot,
    this.onRoomChanged,
    this.onSignOut,
  });

  final WishPoolSnapshot snapshot;
  final VoidCallback? onRoomChanged;
  final VoidCallback? onSignOut;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 120),
      children: [
        Text('${snapshot.childName}的小屋', style: theme.textTheme.headlineLarge),
        if (snapshot.latestMemoryTitle != null) ...[
          const SizedBox(height: WishPoolSpacing.xs),
          Text('最近回忆：${snapshot.latestMemoryTitle}', style: theme.textTheme.bodyMedium),
        ],
        const SizedBox(height: WishPoolSpacing.md),
        AspectRatio(
          aspectRatio: 0.92,
          child: LayoutBuilder(
            builder: (context, constraints) => Container(
              decoration: BoxDecoration(
                color: const Color(0xFFECFDF5),
                border: Border.all(color: WishPoolColors.border),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Stack(
                children: [
                  if (snapshot.roomItems.isEmpty)
                    const Center(
                      child: Padding(
                        padding: EdgeInsets.all(WishPoolSpacing.md),
                        child: Text('完成心愿或纪念册精选后，小屋元素会出现在这里。', textAlign: TextAlign.center),
                      ),
                    ),
                  for (final item in snapshot.roomItems)
                    _RoomItem(
                      label: item.title,
                      icon: _iconFor(item),
                      left: _percentToPixels(item.left, constraints.maxWidth, maxOffset: 148),
                      top: _percentToPixels(item.top, constraints.maxHeight, maxOffset: 54),
                      locked: !item.unlocked,
                    ),
                ],
              ),
            ),
          ),
        ),
        const SizedBox(height: WishPoolSpacing.md),
        OutlinedButton.icon(
          onPressed: () => _showArrangeSheet(context),
          icon: const Icon(Icons.open_with),
          label: const Text('整理小屋摆放'),
        ),
        if (onSignOut != null) ...[
          const SizedBox(height: WishPoolSpacing.sm),
          OutlinedButton.icon(
            onPressed: onSignOut,
            icon: const Icon(Icons.logout_outlined),
            label: const Text('退出登录'),
          ),
        ],
      ],
    );
  }

  void _showArrangeSheet(BuildContext context) {
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (context) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('整理小屋摆放', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
            const SizedBox(height: WishPoolSpacing.sm),
            for (final item in snapshot.roomItems)
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: Icon(_iconFor(item)),
                title: Text(item.title),
                subtitle: Text(item.unlocked ? '已解锁' : '待解锁'),
                trailing: IconButton(
                  tooltip: '移动摆放',
                  onPressed: item.unlocked ? () => _arrangeItem(context, item) : null,
                  icon: const Icon(Icons.open_with),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Future<void> _arrangeItem(BuildContext context, RoomItemData item) async {
    final nextLeft = (item.left + 12) % 84;
    final nextTop = item.top > 70 ? 28.0 : item.top + 8;
    try {
      await WishPoolScope.of(context).arrangeRoomItem(item, left: nextLeft, top: nextTop);
      onRoomChanged?.call();
      if (!context.mounted) return;
      Navigator.pop(context);
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('摆放已保存。')));
    } catch (_) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('保存失败，请稍后再试。')));
    }
  }

  IconData _iconFor(RoomItemData item) {
    final title = item.title;
    if (title.contains('书') || title.contains('阅读')) return Icons.menu_book_outlined;
    if (title.contains('灯') || title.contains('星')) return Icons.light_outlined;
    if (title.contains('照片') || title.contains('心愿')) return Icons.photo_outlined;
    return Icons.bed_outlined;
  }

  double _percentToPixels(double percent, double extent, {required double maxOffset}) {
    final usable = (extent - maxOffset).clamp(0, extent).toDouble();
    return usable * (percent.clamp(0, 100).toDouble() / 100);
  }
}

class _RoomItem extends StatelessWidget {
  const _RoomItem({
    required this.label,
    required this.icon,
    required this.left,
    required this.top,
    this.locked = false,
  });

  final String label;
  final IconData icon;
  final double left;
  final double top;
  final bool locked;

  @override
  Widget build(BuildContext context) {
    return Positioned(
      left: left,
      top: top,
      child: Opacity(
        opacity: locked ? 0.55 : 1,
        child: Container(
          constraints: const BoxConstraints(minHeight: 44),
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
          decoration: BoxDecoration(
            color: Colors.white,
            border: Border.all(color: WishPoolColors.border),
            borderRadius: BorderRadius.circular(8),
            boxShadow: const [BoxShadow(color: Color(0x140F172A), blurRadius: 18, offset: Offset(0, 8))],
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 18, color: WishPoolColors.primary),
              const SizedBox(width: 6),
              Text(label, style: const TextStyle(fontWeight: FontWeight.w800)),
            ],
          ),
        ),
      ),
    );
  }
}
