import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:wishpool_mobile/src/domain/wishpool_snapshot.dart';
import 'package:wishpool_mobile/src/features/wish_screen.dart';

void main() {
  testWidgets('fragment reveal has a deterministic 380ms intermediate state',
      (tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: _RevealHost(),
      ),
    );

    final start = _fragmentColor(tester);

    tester.state<_RevealHostState>(find.byType(_RevealHost)).reveal();
    await tester.pump(Duration.zero);

    // The state change starts the AnimatedContainer at the locked decoration.
    expect(_fragmentColor(tester), start);

    await tester.pump(const Duration(milliseconds: 190));
    final middle = _fragmentColor(tester);

    // easeOutCubic progresses continuously and is already between its endpoints
    // at half the 380ms duration, so the decoration must be visibly intermediate.
    expect(middle, isNot(start));

    await tester.pump(const Duration(milliseconds: 190));
    final end = _fragmentColor(tester);
    expect(middle, isNot(end));
    expect(_fragmentColor(tester), end);
  });
}

class _RevealHost extends StatefulWidget {
  const _RevealHost();

  @override
  State<_RevealHost> createState() => _RevealHostState();
}

class _RevealHostState extends State<_RevealHost> {
  bool _revealed = false;

  void reveal() => setState(() => _revealed = true);

  @override
  Widget build(BuildContext context) {
    return WishScreen(snapshot: _snapshot(currentFragments: _revealed ? 1 : 0));
  }
}

Color _fragmentColor(WidgetTester tester) {
  final decorations = tester
      .widgetList<DecoratedBox>(find.byType(DecoratedBox))
      .map((box) => box.decoration)
      .whereType<BoxDecoration>()
      .where((decoration) => decoration.color != null);
  final decoration = decorations.first;
  return decoration.color!;
}

WishPoolSnapshot _snapshot({required int currentFragments}) {
  return WishPoolSnapshot(
    source: fixtureSnapshot.source,
    childName: fixtureSnapshot.childName,
    wishTitle: fixtureSnapshot.wishTitle,
    wishProgress: currentFragments.toDouble(),
    wishCurrentFragments: currentFragments,
    wishTargetFragments: 1,
    wishImageUrl: null,
    wishFragmentVisualMode: 'grid_reveal',
    wishFragmentRows: 1,
    wishFragmentCols: 1,
    wishFragmentMask: null,
    wishLitIndexes: const <int>[],
    childTasks: fixtureSnapshot.childTasks,
    reviewCards: fixtureSnapshot.reviewCards,
    weeklyPlanRules: fixtureSnapshot.weeklyPlanRules,
    familyName: fixtureSnapshot.familyName,
    roomItems: fixtureSnapshot.roomItems,
    latestMemoryTitle: fixtureSnapshot.latestMemoryTitle,
    unreadNotifications: fixtureSnapshot.unreadNotifications,
    notificationInbox: fixtureSnapshot.notificationInbox,
    notificationPreferences: fixtureSnapshot.notificationPreferences,
    latestFeedback: fixtureSnapshot.latestFeedback,
  );
}
