import 'package:flutter/material.dart';

import '../design/wishpool_theme.dart';

class RoomScreen extends StatelessWidget {
  const RoomScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 120),
      children: [
        Text('星愿小屋', style: theme.textTheme.headlineLarge),
        const SizedBox(height: WishPoolSpacing.md),
        AspectRatio(
          aspectRatio: 0.92,
          child: Container(
            decoration: BoxDecoration(
              color: const Color(0xFFECFDF5),
              border: Border.all(color: WishPoolColors.border),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Stack(
              children: const [
                _RoomItem(label: '树屋小床', icon: Icons.bed_outlined, left: 20, top: 210),
                _RoomItem(label: '星光台灯', icon: Icons.light_outlined, left: 160, top: 86),
                _RoomItem(label: '纪念册书架', icon: Icons.menu_book_outlined, left: 190, top: 260, locked: true),
              ],
            ),
          ),
        ),
        const SizedBox(height: WishPoolSpacing.md),
        OutlinedButton.icon(
          onPressed: () {},
          icon: const Icon(Icons.open_with),
          label: const Text('整理小屋摆放'),
        ),
      ],
    );
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
