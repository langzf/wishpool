export const family = {
  id: "family-sunrise",
  name: "星愿小屋",
  timezone: "Asia/Shanghai",
  members: [
    { id: "parent-1", role: "parent", displayName: "妈妈" },
    { id: "child-1", role: "child", displayName: "小满" }
  ]
};

export const child = {
  id: "child-1",
  familyId: family.id,
  nickname: "小满",
  age: 7,
  roomTheme: "forest",
  status: "active",
  starlightBalance: 42,
  wishFragmentBalance: 6
};

export const todayTasks = [
  {
    id: "task-reading",
    title: "亲子阅读 20 分钟",
    category: "growth",
    submissionType: "photo",
    status: "pending_review",
    isCore: true,
    requireReview: true,
    rewardStarlight: 6
  },
  {
    id: "task-piano",
    title: "钢琴练习",
    category: "skill",
    submissionType: "video",
    status: "todo",
    isCore: true,
    requireReview: true,
    rewardStarlight: 8
  },
  {
    id: "task-kindness",
    title: "记录一件开心的小事",
    category: "habit",
    submissionType: "audio",
    status: "approved",
    isCore: false,
    requireReview: true,
    rewardStarlight: 4
  }
];

export const currentWish = {
  id: "wish-lego",
  childId: child.id,
  title: "周末搭建城市积木",
  description: "完成本周核心任务后，周六下午一起搭城市小屋。",
  status: "active",
  targetFragments: 10,
  currentFragments: 6,
  coverMediaId: "media-wish-cover"
};

export const pendingReviews = [
  {
    submissionId: "submission-reading",
    childName: child.nickname,
    taskTitle: "亲子阅读 20 分钟",
    submittedAt: "2026-08-12T19:10:00+08:00",
    aiSummary: "画面清晰，能看到绘本和阅读环境，建议家长确认阅读时长。",
    riskLevel: "low",
    mediaType: "image"
  },
  {
    submissionId: "submission-audio",
    childName: child.nickname,
    taskTitle: "记录一件开心的小事",
    submittedAt: "2026-08-12T18:42:00+08:00",
    aiSummary: "语音情绪积极，内容与任务匹配。",
    riskLevel: "low",
    mediaType: "audio"
  }
];

export const weeklyPlan = {
  id: "plan-week-33",
  childId: child.id,
  weekStartDate: "2026-08-10",
  status: "active",
  rules: [
    { title: "亲子阅读 20 分钟", weekdays: [1, 2, 3, 4, 5], rewardStarlight: 6 },
    { title: "钢琴练习", weekdays: [1, 3, 5], rewardStarlight: 8 },
    { title: "运动打卡", weekdays: [2, 4, 6], rewardStarlight: 5 }
  ]
};

export const memories = [
  {
    id: "memory-week-32",
    title: "勇敢表达的一周",
    weekStartDate: "2026-08-03",
    summary: "小满完成了阅读、运动和一次语音分享，开始更主动地描述自己的感受。",
    highlights: ["第一次完整讲完故事", "坚持了三次运动", "主动感谢家人"]
  }
];

export const roomState = {
  childId: child.id,
  theme: "forest",
  items: [
    { id: "bed-tree", kind: "bed", name: "树屋小床", x: 16, y: 52, unlocked: true },
    { id: "lamp-star", kind: "lamp", name: "星光台灯", x: 68, y: 28, unlocked: true },
    { id: "shelf-memory", kind: "shelf", name: "纪念册书架", x: 76, y: 62, unlocked: false }
  ]
};

export const privacyQueue = [
  {
    id: "privacy-export-1",
    type: "export",
    requesterName: "妈妈",
    status: "verifying",
    createdAt: "2026-08-12T09:30:00+08:00"
  }
];

export const dashboardMetrics = {
  activeChildren: 1,
  pendingReviews: pendingReviews.length,
  weeklyCompletionRate: 0.76,
  starlightIssuedThisWeek: 86,
  mediaProcessingReadyRate: 0.94
};

export function getFixtureSnapshot() {
  return {
    family,
    child,
    todayTasks,
    currentWish,
    pendingReviews,
    weeklyPlan,
    memories,
    roomState,
    privacyQueue,
    dashboardMetrics
  };
}
