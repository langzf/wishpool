import '../shared/fixture_data.dart';

class WishPoolSnapshot {
  const WishPoolSnapshot({
    required this.source,
    required this.childName,
    required this.wishTitle,
    required this.wishProgress,
    required this.wishCurrentFragments,
    required this.wishTargetFragments,
    required this.wishImageUrl,
    required this.wishFragmentVisualMode,
    required this.wishFragmentRows,
    required this.wishFragmentCols,
    required this.wishFragmentMask,
    required this.wishLitIndexes,
    required this.childTasks,
    required this.reviewCards,
    required this.weeklyPlanRules,
    required this.familyName,
    required this.roomItems,
    required this.latestMemoryTitle,
    required this.unreadNotifications,
    required this.notificationInbox,
    required this.notificationPreferences,
  });

  final String source;
  final String childName;
  final String wishTitle;
  final double wishProgress;
  final int wishCurrentFragments;
  final int wishTargetFragments;
  final String? wishImageUrl;
  final String wishFragmentVisualMode;
  final int? wishFragmentRows;
  final int? wishFragmentCols;
  final WishFragmentMaskData? wishFragmentMask;
  final List<int> wishLitIndexes;
  final List<ChildTask> childTasks;
  final List<ReviewCardData> reviewCards;
  final List<WeeklyPlanRuleData> weeklyPlanRules;
  final String familyName;
  final List<RoomItemData> roomItems;
  final String? latestMemoryTitle;
  final int unreadNotifications;
  final List<NotificationItemData> notificationInbox;
  final List<NotificationPreferenceData> notificationPreferences;
}

const fixtureSnapshot = WishPoolSnapshot(
  source: 'fixture',
  childName: childName,
  wishTitle: wishTitle,
  wishProgress: wishProgress,
  wishCurrentFragments: wishCurrentFragments,
  wishTargetFragments: wishTargetFragments,
  wishImageUrl: null,
  wishFragmentVisualMode: 'puzzle_lines',
  wishFragmentRows: null,
  wishFragmentCols: null,
  wishFragmentMask: null,
  wishLitIndexes: const <int>[],
  childTasks: childTasks,
  reviewCards: reviewCards,
  weeklyPlanRules: weeklyPlanRules,
  familyName: familyName,
  roomItems: roomItems,
  latestMemoryTitle: latestMemoryTitle,
  unreadNotifications: 0,
  notificationInbox: notificationInbox,
  notificationPreferences: notificationPreferences,
);

class WishFragmentMaskData {
  const WishFragmentMaskData({
    required this.version,
    required this.mode,
    required this.rows,
    required this.cols,
    required this.total,
    required this.revealOrder,
    required this.cells,
  });

  final int version;
  final String mode;
  final int rows;
  final int cols;
  final int total;
  final List<int> revealOrder;
  final List<WishFragmentCellData> cells;
}

class WishFragmentCellData {
  const WishFragmentCellData({
    required this.index,
    required this.row,
    required this.col,
    this.polygon,
  });

  final int index;
  final int row;
  final int col;
  final List<WishFragmentPointData>? polygon;
}

class WishFragmentPointData {
  const WishFragmentPointData({required this.x, required this.y});

  final double x;
  final double y;
}
