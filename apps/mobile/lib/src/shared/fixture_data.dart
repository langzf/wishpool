class ChildTask {
  const ChildTask({
    required this.title,
    required this.category,
    required this.status,
    required this.reward,
    required this.submissionType,
  });

  final String title;
  final String category;
  final String status;
  final int reward;
  final String submissionType;
}

class ReviewCardData {
  const ReviewCardData({
    required this.title,
    required this.summary,
    required this.mediaType,
  });

  final String title;
  final String summary;
  final String mediaType;
}

const childName = '小满';
const wishTitle = '周末搭建城市积木';
const wishProgress = 0.6;

const childTasks = [
  ChildTask(
    title: '亲子阅读 20 分钟',
    category: '成长',
    status: '待审核',
    reward: 6,
    submissionType: '照片',
  ),
  ChildTask(
    title: '钢琴练习',
    category: '技能',
    status: '待打卡',
    reward: 8,
    submissionType: '视频',
  ),
  ChildTask(
    title: '记录一件开心的小事',
    category: '习惯',
    status: '已通过',
    reward: 4,
    submissionType: '语音',
  ),
];

const reviewCards = [
  ReviewCardData(
    title: '亲子阅读 20 分钟',
    summary: '画面清晰，能看到绘本和阅读环境，建议确认阅读时长。',
    mediaType: '照片',
  ),
  ReviewCardData(
    title: '记录一件开心的小事',
    summary: '语音情绪积极，内容与任务匹配。',
    mediaType: '语音',
  ),
];
