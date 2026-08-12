import '../shared/fixture_data.dart';

class WishPoolSnapshot {
  const WishPoolSnapshot({
    required this.source,
    required this.childName,
    required this.wishTitle,
    required this.wishProgress,
    required this.childTasks,
    required this.reviewCards,
  });

  final String source;
  final String childName;
  final String wishTitle;
  final double wishProgress;
  final List<ChildTask> childTasks;
  final List<ReviewCardData> reviewCards;
}

const fixtureSnapshot = WishPoolSnapshot(
  source: 'fixture',
  childName: childName,
  wishTitle: wishTitle,
  wishProgress: wishProgress,
  childTasks: childTasks,
  reviewCards: reviewCards,
);
