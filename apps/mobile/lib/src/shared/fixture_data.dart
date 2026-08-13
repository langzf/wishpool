class ChildTask {
  const ChildTask({
    required this.id,
    required this.title,
    required this.category,
    required this.status,
    required this.reward,
    required this.submissionType,
    required this.submissionTypeCode,
  });

  final String id;
  final String title;
  final String category;
  final String status;
  final int reward;
  final String submissionType;
  final String submissionTypeCode;
}

class WeeklyPlanRuleData {
  const WeeklyPlanRuleData({
    required this.title,
    required this.category,
    required this.submissionType,
    required this.weekdays,
    required this.isCore,
  });

  final String title;
  final String category;
  final String submissionType;
  final List<int> weekdays;
  final bool isCore;
}

class ReviewCardData {
  const ReviewCardData({
    required this.submissionId,
    required this.title,
    required this.summary,
    required this.mediaType,
  });

  final String submissionId;
  final String title;
  final String summary;
  final String mediaType;
}

class RoomItemData {
  const RoomItemData({
    required this.id,
    required this.title,
    required this.left,
    required this.top,
    required this.unlocked,
  });

  final String id;
  final String title;
  final double left;
  final double top;
  final bool unlocked;
}

class NotificationItemData {
  const NotificationItemData({
    required this.id,
    required this.title,
    required this.body,
    required this.status,
    required this.createdAt,
  });

  final String id;
  final String title;
  final String body;
  final String status;
  final String createdAt;
}

class NotificationPreferenceData {
  const NotificationPreferenceData({
    required this.type,
    required this.enabled,
  });

  final String type;
  final bool enabled;
}

const familyName = '星愿小屋';
const childName = '小满';
const wishTitle = '周末搭建城市积木';
const wishProgress = 0.6;
const wishCurrentFragments = 6;
const wishTargetFragments = 10;
const latestMemoryTitle = '勇敢表达的一周';

const childTasks = [
  ChildTask(
    id: 'task-reading',
    title: '亲子阅读 20 分钟',
    category: '成长',
    status: '待审核',
    reward: 6,
    submissionType: '照片',
    submissionTypeCode: 'photo',
  ),
  ChildTask(
    id: 'task-piano',
    title: '钢琴练习',
    category: '技能',
    status: '待打卡',
    reward: 8,
    submissionType: '视频',
    submissionTypeCode: 'video',
  ),
  ChildTask(
    id: 'task-kindness',
    title: '记录一件开心的小事',
    category: '习惯',
    status: '已通过',
    reward: 4,
    submissionType: '语音',
    submissionTypeCode: 'audio',
  ),
];

const reviewCards = [
  ReviewCardData(
    submissionId: 'submission-reading',
    title: '亲子阅读 20 分钟',
    summary: '画面清晰，能看到绘本和阅读环境，建议确认阅读时长。',
    mediaType: '照片',
  ),
  ReviewCardData(
    submissionId: 'submission-audio',
    title: '记录一件开心的小事',
    summary: '语音情绪积极，内容与任务匹配。',
    mediaType: '语音',
  ),
];

const weeklyPlanRules = [
  WeeklyPlanRuleData(
    title: '亲子阅读 20 分钟',
    category: '成长',
    submissionType: '照片',
    weekdays: [1, 2, 3, 4, 5],
    isCore: true,
  ),
  WeeklyPlanRuleData(
    title: '钢琴练习',
    category: '技能',
    submissionType: '视频',
    weekdays: [2, 4, 6],
    isCore: true,
  ),
  WeeklyPlanRuleData(
    title: '记录一件开心的小事',
    category: '习惯',
    submissionType: '语音',
    weekdays: [1, 3, 5],
    isCore: false,
  ),
];

const roomItems = [
  RoomItemData(id: 'bed-tree', title: '树屋小床', left: 20, top: 210, unlocked: true),
  RoomItemData(id: 'lamp-star', title: '星光台灯', left: 160, top: 86, unlocked: true),
  RoomItemData(id: 'shelf-memory', title: '纪念册书架', left: 190, top: 260, unlocked: false),
];

const notificationInbox = [
  NotificationItemData(
    id: 'notification-review',
    title: '任务通过啦',
    body: '亲子阅读记录已经通过，星光已进入本周进度。',
    status: 'pending',
    createdAt: '19:20',
  ),
  NotificationItemData(
    id: 'notification-plan',
    title: '今日任务有更新',
    body: '钢琴练习已顺延到明天。',
    status: 'sent',
    createdAt: '18:40',
  ),
];

const notificationPreferences = [
  NotificationPreferenceData(type: 'child_submission_created', enabled: true),
  NotificationPreferenceData(type: 'review_completed', enabled: true),
  NotificationPreferenceData(type: 'wish_unlocked', enabled: true),
  NotificationPreferenceData(type: 'task_plan_changed', enabled: true),
];
