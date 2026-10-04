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
    this.latestFeedback,
    this.wishHistory = const <WishHistoryItemData>[],
    this.memoryTimeline = const <MemoryTimelineItemData>[],
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
  final ChildFeedbackData? latestFeedback;
  final List<WishHistoryItemData> wishHistory;
  final List<MemoryTimelineItemData> memoryTimeline;
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
  wishLitIndexes: <int>[],
  childTasks: childTasks,
  reviewCards: reviewCards,
  weeklyPlanRules: weeklyPlanRules,
  familyName: familyName,
  roomItems: roomItems,
  latestMemoryTitle: latestMemoryTitle,
  unreadNotifications: 0,
  notificationInbox: notificationInbox,
  notificationPreferences: notificationPreferences,
  latestFeedback: null,
);

class WishHistoryItemData {
  const WishHistoryItemData({
    required this.id,
    required this.title,
    required this.status,
    required this.earnedFragments,
    required this.requiredFragments,
    this.imageUrl,
    this.redeemedDate,
    this.redemptionPhotoUrls = const <String>[],
  });

  final String id;
  final String title;
  final String status;
  final int earnedFragments;
  final int requiredFragments;
  final String? imageUrl;
  final String? redeemedDate;
  final List<String> redemptionPhotoUrls;

  bool get redeemed => redeemedDate != null && redeemedDate!.isNotEmpty;
}

class MemoryTimelineItemData {
  const MemoryTimelineItemData({
    required this.id,
    required this.weekId,
    required this.title,
    required this.status,
    required this.summary,
    required this.items,
  });

  final String id;
  final String weekId;
  final String title;
  final String status;
  final String summary;
  final List<MemoryMediaItemData> items;
}

class MemoryMediaItemData {
  const MemoryMediaItemData({
    required this.id,
    required this.title,
    required this.kind,
    this.sourceType,
    this.sourceId,
    this.mediaAssetId,
    this.contentType,
    this.url,
    this.thumbnailUrl,
    this.note,
    this.scheduledDate,
    this.durationSec,
  });

  final String id;
  final String title;
  final String kind;
  final String? sourceType;
  final String? sourceId;
  final String? mediaAssetId;
  final String? contentType;
  final String? url;
  final String? thumbnailUrl;
  final String? note;
  final String? scheduledDate;
  final int? durationSec;

  bool get playable => url != null && url!.isNotEmpty;
}

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
