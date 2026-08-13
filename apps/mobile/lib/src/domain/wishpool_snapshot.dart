import '../shared/fixture_data.dart';

class WishPoolSnapshot {
  const WishPoolSnapshot({
    required this.source,
    required this.childName,
    required this.wishTitle,
    required this.wishProgress,
    required this.wishCurrentFragments,
    required this.wishTargetFragments,
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
