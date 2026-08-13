import {
  child,
  currentWish,
  dashboardMetrics,
  memories,
  notificationInbox,
  notificationPreferences,
  pendingReviews,
  roomState,
  todayTasks,
  weeklyPlan
} from "@wishpool/app-fixtures";
import { buildAuthHeaders, getApiRuntimeConfig } from "@/lib/api";
import { configForParentWebSession, type ParentWebSession } from "@/lib/session";

export type ParentDashboardData = {
  source: "api" | "fixture";
  child: typeof child;
  currentWish: typeof currentWish;
  dashboardMetrics: typeof dashboardMetrics;
  memories: typeof memories;
  pendingReviews: typeof pendingReviews;
  roomState: typeof roomState;
  todayTasks: typeof todayTasks;
  weeklyPlan: typeof weeklyPlan;
  notificationInbox: typeof notificationInbox;
  notificationPreferences: typeof notificationPreferences;
  coreApiHealthy: boolean;
};

export async function loadParentDashboardData(session?: ParentWebSession | null): Promise<ParentDashboardData> {
  const config = configForParentWebSession(session);
  const fixture = fixtureDashboardData(false);
  if (!config.accessToken || !config.familyId) return fixture;

  try {
    const childQuery = config.childId ? `?childId=${encodeURIComponent(config.childId)}` : "";
    const response = await fetch(`${config.coreApiBaseUrl}/families/${config.familyId}/parent-dashboard${childQuery}`, {
      cache: "no-store",
      headers: buildAuthHeaders(config.accessToken)
    });
    if (!response.ok) return fixture;
    const dashboard = (await response.json()) as ParentDashboardContext;
    const preferences = await loadNotificationPreferences(config.coreApiBaseUrl, config.familyId, config.accessToken);
    const selectedChild = mapChild(dashboard.selectedChild) ?? fixture.child;
    return {
      child: selectedChild,
      currentWish: mapWish(dashboard.currentWish) ?? emptyWish(selectedChild.id),
      dashboardMetrics: {
        ...fixture.dashboardMetrics,
        activeChildren: dashboard.children.length,
        pendingReviews: dashboard.pendingReviews.length,
        weeklyCompletionRate: completionRate(dashboard.today),
        starlightIssuedThisWeek: totalApprovedTasks(dashboard.today) * 6
      },
      memories: dashboard.memories.map(mapMemory),
      pendingReviews: dashboard.pendingReviews.map((review) => mapReview(review, selectedChild.nickname)),
      roomState: mapRoomState(dashboard.room, selectedChild.id) ?? roomState,
      todayTasks: dashboard.today?.tasks.map(mapTask) ?? [],
      weeklyPlan: mapWeeklyPlan(dashboard.weeklyPlan) ?? emptyWeeklyPlan(selectedChild.id),
      notificationInbox: dashboard.notificationInbox.items.map(mapNotification),
      notificationPreferences: preferences.length > 0 ? preferences.map(mapPreference) : fixture.notificationPreferences,
      source: "api",
      coreApiHealthy: true
    };
  } catch {
    return fixture;
  }
}

function fixtureDashboardData(coreApiHealthy: boolean): ParentDashboardData {
  return {
    source: "fixture",
    child,
    currentWish,
    dashboardMetrics,
    memories,
    pendingReviews,
    roomState,
    todayTasks,
    weeklyPlan,
    notificationInbox,
    notificationPreferences,
    coreApiHealthy
  };
}

type ParentDashboardContext = {
  family: { id: string; name: string; timezone: string; status: string };
  children: Array<{ id: string; familyId: string; nickname: string; roomTheme: string; status: string }>;
  selectedChild: { id: string; familyId: string; nickname: string; roomTheme: string; status: string } | null;
  today: { tasks: TaskInstance[]; dailySummary?: { coreRequired: number; coreApproved: number; coreSkipped: number } | null } | null;
  currentWish: WishResponse | null;
  pendingReviews: PendingReviewResponse[];
  weeklyPlan: WeeklyPlanResponse | null;
  taskTemplates: TaskTemplateResponse[];
  memories: Array<{ id: string; title: string; weekId: string; summary?: Record<string, unknown> }>;
  room: RoomStateResponse | null;
  notificationInbox: {
    items: NotificationEventResponse[];
  };
};

type TaskInstance = {
  id: string;
  title: string;
  category: string;
  submissionType: string;
  status: string;
  scheduledDate: string;
  isCore: boolean;
  requireReview: boolean;
};

type WishResponse = {
  id: string;
  childId: string;
  title: string;
  note?: string | null;
  status: string;
  requiredFragments: number;
  earnedFragments: number;
};

type PendingReviewResponse = {
  submission: { id: string; submittedAt: string; submissionType: string };
  task: { title: string };
  aiSummary?: string | null;
};

type WeeklyPlanResponse = {
  id: string;
  childId: string;
  weekId: string;
  startDate: string;
  endDate: string;
  rewardMode: string;
  status: string;
  rules: Array<{
    title: string;
    category: string;
    submissionType: string;
    weekdays: number[];
    isCore: boolean;
    requireReview: boolean;
  }>;
};

type TaskTemplateResponse = {
  id: string;
  title: string;
  category: string;
  submissionType: string;
};

type RoomStateResponse = {
  childId: string;
  theme?: string | null;
  items: Array<{
    id: string;
    type: string;
    title: string;
    visible: boolean;
    position?: Record<string, unknown> | null;
  }>;
};

type NotificationEventResponse = {
  id: string;
  type: string;
  title: string;
  body: string;
  status: string;
  createdAt: string;
};

type NotificationPreferenceResponse = {
  notificationType: string;
  enabled: boolean;
  channels: Record<string, boolean>;
};

function mapChild(value: ParentDashboardContext["selectedChild"]) {
  if (!value) return null;
  return {
    id: value.id,
    familyId: value.familyId,
    nickname: value.nickname,
    age: 7,
    roomTheme: value.roomTheme,
    status: value.status,
    starlightBalance: 0,
    wishFragmentBalance: 0
  };
}

function mapTask(task: TaskInstance) {
  return {
    id: task.id,
    title: task.title,
    category: task.category,
    submissionType: task.submissionType,
    status: task.status,
    scheduledDate: task.scheduledDate,
    isCore: task.isCore,
    requireReview: task.requireReview,
    rewardStarlight: task.isCore ? 8 : 4
  };
}

function mapWish(wish: WishResponse | null) {
  if (!wish) return null;
  return {
    id: wish.id,
    childId: wish.childId,
    title: wish.title,
    description: wish.note ?? "本周心愿正在收集小星光。",
    status: wish.status,
    targetFragments: wish.requiredFragments,
    currentFragments: wish.earnedFragments,
    coverMediaId: wish.id
  };
}

function emptyWish(childId: string) {
  return {
    id: "",
    childId,
    title: "还没有本周心愿",
    description: "创建一个本周心愿后，孩子完成核心任务会推进碎片进度。",
    status: "draft",
    targetFragments: 1,
    currentFragments: 0,
    coverMediaId: ""
  };
}

function mapReview(review: PendingReviewResponse, childName: string) {
  return {
    submissionId: review.submission.id,
    childName,
    taskTitle: review.task.title,
    submittedAt: review.submission.submittedAt,
    aiSummary: review.aiSummary ?? "AI 预审暂未完成，请查看提交内容。",
    riskLevel: "low",
    mediaType: review.submission.submissionType
  };
}

function mapMemory(memory: ParentDashboardContext["memories"][number]) {
  const summary = typeof memory.summary?.summary === "string" ? memory.summary.summary : "本周成长记录已生成。";
  return {
    id: memory.id,
    title: memory.title,
    weekStartDate: memory.weekId,
    summary,
    highlights: []
  };
}

function mapWeeklyPlan(plan: WeeklyPlanResponse | null) {
  if (!plan) return null;
  return {
    id: plan.id,
    childId: plan.childId,
    weekId: plan.weekId,
    weekStartDate: plan.startDate,
    weekEndDate: plan.endDate,
    rewardMode: plan.rewardMode,
    status: plan.status,
    rules: plan.rules.map((rule) => ({
      title: rule.title,
      category: rule.category,
      submissionType: rule.submissionType,
      weekdays: rule.weekdays,
      isCore: rule.isCore,
      requireReview: rule.requireReview,
      rewardStarlight: 6
    }))
  };
}

function emptyWeeklyPlan(childId: string) {
  const week = currentWeek();
  return {
    id: "",
    childId,
    weekId: week.weekId,
    weekStartDate: week.startDate,
    weekEndDate: week.endDate,
    rewardMode: "flexible",
    status: "draft",
    rules: []
  };
}

function mapRoomState(state: RoomStateResponse | null, childId: string) {
  if (!state) return null;
  return {
    childId,
    theme: state.theme ?? "forest",
    items: state.items.map((item, index) => {
      const position = item.position ?? {};
      return {
        id: item.id,
        kind: item.type,
        name: item.title,
        x: numberValue(position.x, numberValue(position.left, 16 + index * 18)),
        y: numberValue(position.y, numberValue(position.top, 32 + index * 10)),
        unlocked: item.visible
      };
    })
  };
}

function mapNotification(notification: NotificationEventResponse) {
  return {
    id: notification.id,
    type: notification.type,
    title: notification.title,
    body: notification.body,
    status: notification.status,
    createdAt: notification.createdAt
  };
}

function mapPreference(preference: NotificationPreferenceResponse) {
  return {
    notificationType: preference.notificationType,
    enabled: preference.enabled,
    channels: preference.channels
  };
}

async function loadNotificationPreferences(coreApiBaseUrl: string, familyId: string, accessToken?: string) {
  if (!accessToken) return [];
  const response = await fetch(`${coreApiBaseUrl}/notification-preferences?familyId=${familyId}`, {
    cache: "no-store",
    headers: buildAuthHeaders(accessToken)
  });
  if (!response.ok) return [];
  return (await response.json()) as NotificationPreferenceResponse[];
}

function completionRate(todayData: ParentDashboardContext["today"]) {
  if (!todayData || todayData.tasks.length === 0) return 0;
  return todayData.tasks.filter((task) => task.status === "approved" || task.status === "skipped").length / todayData.tasks.length;
}

function totalApprovedTasks(todayData: ParentDashboardContext["today"]) {
  return todayData?.tasks.filter((task) => task.status === "approved").length ?? 0;
}

function numberValue(value: unknown, fallback: number) {
  return typeof value === "number" && Number.isFinite(value) ? value : fallback;
}

function currentWeek() {
  const now = new Date();
  const day = now.getUTCDay() || 7;
  const monday = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()));
  monday.setUTCDate(monday.getUTCDate() - day + 1);
  const sunday = new Date(monday);
  sunday.setUTCDate(monday.getUTCDate() + 6);
  const thursday = new Date(monday);
  thursday.setUTCDate(monday.getUTCDate() + 3);
  const start = new Date(Date.UTC(thursday.getUTCFullYear(), 0, 1));
  const weekNumber = Math.ceil((((thursday.getTime() - start.getTime()) / 86400000) + 1) / 7);
  return {
    weekId: `${thursday.getUTCFullYear()}-W${String(weekNumber).padStart(2, "0")}`,
    startDate: monday.toISOString().slice(0, 10),
    endDate: sunday.toISOString().slice(0, 10)
  };
}
