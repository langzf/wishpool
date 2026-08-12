import 'package:flutter/material.dart';

import '../data/wishpool_scope.dart';
import '../design/wishpool_theme.dart';
import '../domain/wishpool_snapshot.dart';
import 'child_dashboard.dart';
import 'parent_dashboard.dart';
import 'room_screen.dart';
import 'wish_screen.dart';

class MobileHomeScreen extends StatefulWidget {
  const MobileHomeScreen({super.key});

  @override
  State<MobileHomeScreen> createState() => _MobileHomeScreenState();
}

class _MobileHomeScreenState extends State<MobileHomeScreen> {
  int _index = 0;
  late Future<WishPoolSnapshot> _snapshotFuture;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _snapshotFuture = WishPoolScope.of(context).loadSnapshot();
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<WishPoolSnapshot>(
      future: _snapshotFuture,
      builder: (context, snapshot) {
        final data = snapshot.data ?? fixtureSnapshot;
        final screens = [
          ChildDashboardScreen(snapshot: data),
          WishScreen(snapshot: data),
          const RoomScreen(),
          ParentDashboardScreen(snapshot: data),
        ];
        return Scaffold(
          body: SafeArea(child: screens[_index]),
          bottomNavigationBar: NavigationBar(
            selectedIndex: _index,
            onDestinationSelected: (value) => setState(() => _index = value),
            destinations: const [
              NavigationDestination(icon: Icon(Icons.check_circle_outline), label: '任务'),
              NavigationDestination(icon: Icon(Icons.favorite_border), label: '心愿'),
              NavigationDestination(icon: Icon(Icons.home_outlined), label: '小屋'),
              NavigationDestination(icon: Icon(Icons.verified_outlined), label: '家长'),
            ],
          ),
          floatingActionButton: _index == 0
              ? FloatingActionButton.extended(
                  onPressed: () {},
                  icon: const Icon(Icons.add_a_photo_outlined),
                  label: const Text('打卡'),
                  backgroundColor: WishPoolColors.primary,
                  foregroundColor: WishPoolColors.onPrimary,
                )
              : null,
        );
      },
    );
  }
}
