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
import { coreGetJson } from "@/lib/core-client";
import { configForParentWebSession, type ParentWebSession } from "@/lib/session";
import type { FragmentMask } from "@/components/FragmentGrid";

export type ParentDashboardData = {
  source: "api" | "fixture";
  child: typeof child;
  currentWish: WishSummary;
  wishHistory: WishHistoryItem[];
  dashboardMetrics: typeof dashboardMetrics;
  memories: typeof memories;
  pendingReviews: typeof pendingReviews;
  roomState: typeof roomState;
  taskTemplates: TaskTemplateResponse[];
  todayTasks: typeof todayTasks;
  weeklyPlan: typeof weeklyPlan;
  notificationInbox: typeof notificationInbox;
  notificationPreferences: ParentNotificationPreference[];
  coreApiHealthy: boolean;
  coreApiError?: string;
};

export class ParentWebUnauthorizedError extends Error {
  constructor() {
    super("Parent web session is no longer authorized.");
    this.name = "ParentWebUnauthorizedError";
  }
}

export class ParentDashboardUnavailableError extends Error {
  constructor(message = "核心服务暂时不可用，请稍后重试。") {
    super(message);
    this.name = "ParentDashboardUnavailableError";
  }
}

export function isParentWebUnauthorizedError(error: unknown): error is ParentWebUnauthorizedError {
  return error instanceof ParentWebUnauthorizedError;
}

export async function loadParentDashboardData(session?: ParentWebSession | null): Promise<ParentDashboardData> {
  const config = configForParentWebSession(session);
  if (config.demoMode) return fixtureDashboardData(true);
  if (!config.accessToken || !config.familyId) {
    throw new ParentDashboardUnavailableError("登录会话缺少家庭信息，请重新登录后重试。");
  }
  const fixture = fixtureDashboardData(false);

  try {
    const childQuery = config.childId ? `?childId=${encodeURIComponent(config.childId)}` : "";
    const dashboard = await coreGetJson<ParentDashboardContext>(`/families/${config.familyId}/parent-dashboard${childQuery}`, config.accessToken);
    const preferences = await loadNotificationPreferences(config.familyId, config.accessToken);
    const selectedChild = mapChild(dashboard.selectedChild) ?? fixture.child;
    return {
      child: selectedChild,
      currentWish: mapWish(dashboard.currentWish) ?? emptyWish(selectedChild.id),
      wishHistory: (dashboard.wishHistory ?? []).map(mapWishHistoryItem),
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
      taskTemplates: dashboard.taskTemplates,
      todayTasks: dashboard.today?.tasks.map(mapTask) ?? [],
      weeklyPlan: mapWeeklyPlan(dashboard.weeklyPlan) ?? emptyWeeklyPlan(selectedChild.id),
      notificationInbox: dashboard.notificationInbox.items.map(mapNotification),
      notificationPreferences: normalizeNotificationPreferences(preferences),
      source: "api",
      coreApiHealthy: true
    };
  } catch (error) {
    if (isParentWebUnauthorizedError(error)) throw error;
    if (error instanceof Error && error.name === "CoreApiUnauthorizedError") {
      throw error;
    }
    if (error instanceof ParentDashboardUnavailableError) throw error;
    throw new ParentDashboardUnavailableError(`服务暂时不可用：${errorMessage(error)}`);
  }
}

function fixtureDashboardData(coreApiHealthy: boolean, coreApiError?: string): ParentDashboardData {
  return {
    source: "fixture",
    child,
    currentWish: fixtureCurrentWish(child.id),
    wishHistory: fixtureWishHistory(child.id),
    dashboardMetrics,
    memories,
    pendingReviews,
    roomState,
    taskTemplates: [],
    todayTasks,
    weeklyPlan,
    notificationInbox,
    notificationPreferences: normalizeNotificationPreferences(notificationPreferences),
    coreApiHealthy,
    coreApiError
  };
}

function fixtureCurrentWish(childId: string): WishSummary {
  return {
    id: currentWish.id,
    childId,
    title: currentWish.title,
    description: currentWish.description,
    status: currentWish.status,
    targetFragments: currentWish.targetFragments,
    currentFragments: currentWish.currentFragments,
    coverMediaId: currentWish.coverMediaId,
    fragmentVisualMode: "puzzle_lines"
  };
}

export function loadReviewsData(data: ParentDashboardData) {
  return {
    pendingReviews: data.pendingReviews
  };
}

export function loadPlanData(data: ParentDashboardData) {
  return {
    weeklyPlan: data.weeklyPlan,
    todayTasks: data.todayTasks,
    taskTemplates: data.taskTemplates
  };
}

export function loadWishData(data: ParentDashboardData) {
  return {
    currentWish: data.currentWish,
    wishHistory: data.wishHistory
  };
}

export function loadMemoriesData(data: ParentDashboardData) {
  return {
    memories: data.memories
  };
}

export function loadRoomData(data: ParentDashboardData) {
  return {
    roomState: data.roomState
  };
}

export function loadNotificationsData(data: ParentDashboardData) {
  return {
    notificationInbox: data.notificationInbox,
    notificationPreferences: data.notificationPreferences
  };
}

type ParentDashboardContext = {
  family: { id: string; name: string; timezone: string; status: string };
  children: Array<{ id: string; familyId: string; nickname: string; roomTheme: string; status: string }>;
  selectedChild: { id: string; familyId: string; nickname: string; roomTheme: string; status: string } | null;
  today: { tasks: TaskInstance[]; dailySummary?: { coreRequired: number; coreApproved: number; coreSkipped: number } | null } | null;
  currentWish: WishResponse | null;
  wishHistory?: WishHistoryItemResponse[];
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
  weekId?: string;
  imageMedia?: MediaAssetResponse | null;
  fragmentVisual?: WishFragmentVisualResponse | null;
};

type WishFragmentVisualResponse = {
  mode: string;
  rows: number;
  cols: number;
  revealed: number;
  total: number;
  mask?: FragmentMask | null;
  litIndexes?: number[] | null;
};

type MediaAssetResponse = {
  id: string;
  contentType: string;
  status: string;
  downloadUrl?: string | null;
  storageKey?: string | null;
};

type WishRedemptionResponse = {
  id: string;
  wishId: string;
  redeemedDate: string;
  parentNote?: string | null;
  childNote?: string | null;
  photos: MediaAssetResponse[];
  createdAt: string;
};

type WishHistoryItemResponse = {
  wish: WishResponse;
  redemption?: WishRedemptionResponse | null;
  realizedCount: number;
};

export type WishSummary = {
  id: string;
  childId: string;
  title: string;
  description: string;
  status: string;
  targetFragments: number;
  currentFragments: number;
  coverMediaId: string;
  imageUrl?: string;
  fragmentVisualMode: string;
  fragmentRows?: number;
  fragmentCols?: number;
  fragmentMask?: FragmentMask;
  litIndexes?: number[];
};

export type WishHistoryItem = WishSummary & {
  weekId: string;
  realizedCount: number;
  redemption?: {
    id: string;
    redeemedDate: string;
    parentNote?: string;
    childNote?: string;
    photos: Array<{ id: string; contentType: string; downloadUrl?: string }>;
    createdAt: string;
  };
};

type PendingReviewResponse = {
  submission: { id: string; submittedAt: string; submissionType: string };
  task: { title: string; category: string; isCore: boolean };
  aiSummary?: string | null;
  thumbnailMedia?: MediaAssetResponse | null;
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

export type TaskTemplateResponse = {
  id: string;
  familyId: string;
  title: string;
  category: string;
  submissionType: string;
  description?: string | null;
  targetText?: string | null;
  defaultDurationSec?: number | null;
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
  quietHours?: Record<string, unknown> | null;
};

export type ParentNotificationPreference = {
  notificationType: string;
  enabled: boolean;
  channels: { inbox: boolean; push: boolean };
  quietHours: { start?: string; end?: string; timezone?: string };
  saved: boolean;
};

const NOTIFICATION_TYPES = [
  "child_submission_created",
  "ai_precheck_completed",
  "review_completed",
  "wish_fragment_earned",
  "wish_unlocked",
  "wish_redeemed_memory_generated",
  "task_plan_changed"
] as const;

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
    coverMediaId: wish.imageMedia?.id ?? wish.id,
    imageUrl: wish.imageMedia?.downloadUrl ?? undefined,
    fragmentVisualMode: wish.fragmentVisual?.mode ?? "grid_reveal",
    fragmentRows: wish.fragmentVisual?.rows,
    fragmentCols: wish.fragmentVisual?.cols,
    fragmentMask: wish.fragmentVisual?.mask ?? undefined,
    litIndexes: wish.fragmentVisual?.litIndexes ?? undefined
  };
}

function mapWishHistoryItem(item: WishHistoryItemResponse): WishHistoryItem {
  const wish = mapWish(item.wish) ?? emptyWish(item.wish.childId);
  return {
    ...wish,
    weekId: item.wish.weekId ?? "",
    realizedCount: item.realizedCount,
    redemption: item.redemption
      ? {
          id: item.redemption.id,
          redeemedDate: item.redemption.redeemedDate,
          parentNote: item.redemption.parentNote ?? undefined,
          childNote: item.redemption.childNote ?? undefined,
          photos: item.redemption.photos.map((photo) => ({
            id: photo.id,
            contentType: photo.contentType,
            downloadUrl: photo.downloadUrl ?? undefined
          })),
          createdAt: item.redemption.createdAt
        }
      : undefined
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
    coverMediaId: "",
    fragmentVisualMode: "grid_reveal"
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
    mediaType: review.submission.submissionType,
    category: review.task.category,
    isCore: review.task.isCore,
    thumbnailMedia: review.thumbnailMedia
      ? { id: review.thumbnailMedia.id, contentType: review.thumbnailMedia.contentType, downloadUrl: review.thumbnailMedia.downloadUrl ?? null }
      : null
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
    channels: {
      inbox: preference.channels.inbox ?? true,
      push: preference.channels.push ?? false
    },
    quietHours: normalizeQuietHours(preference.quietHours),
    saved: true
  };
}

function normalizeQuietHours(value: Record<string, unknown> | null | undefined) {
  return {
    start: typeof value?.start === "string" ? value.start : undefined,
    end: typeof value?.end === "string" ? value.end : undefined,
    timezone: typeof value?.timezone === "string" ? value.timezone : undefined
  };
}

function normalizeNotificationPreferences(preferences: NotificationPreferenceResponse[]) {
  const byType = new Map(preferences.map((preference) => [preference.notificationType, mapPreference(preference)]));
  return NOTIFICATION_TYPES.map((notificationType) => byType.get(notificationType) ?? {
    notificationType,
    enabled: true,
    channels: { inbox: true, push: true },
    quietHours: {},
    saved: false
  });
}

function fixtureWishHistory(childId: string): WishHistoryItem[] {
  return [
    {
      id: currentWish.id,
      childId,
      weekId: "2026-W34",
      title: currentWish.title,
      description: currentWish.description,
      status: currentWish.status,
      targetFragments: currentWish.targetFragments,
      currentFragments: currentWish.currentFragments,
      coverMediaId: currentWish.coverMediaId,
      fragmentVisualMode: "puzzle_lines",
      realizedCount: 1
    },
    {
      id: "wish-city-blocks-redeemed",
      childId,
      weekId: "2026-W29",
      title: "周末搭建城市积木",
      description: "完成核心任务后，一起搭建新的积木街区。",
      status: "redeemed",
      targetFragments: 10,
      currentFragments: 10,
      coverMediaId: "fixture-city-blocks",
      fragmentVisualMode: "irregular",
      realizedCount: 1,
      redemption: {
        id: "redemption-city-blocks",
        redeemedDate: "2026-07-19",
        parentNote: "小满把道路和图书馆都设计好了，还给每个角色安排了工作。",
        childNote: "下次我想加一个大桥。",
        photos: [],
        createdAt: "2026-07-19T12:00:00Z"
      }
    },
    {
      id: "wish-park-picnic",
      childId,
      weekId: "2026-W27",
      title: "公园野餐",
      description: "集满碎片后，周末去公园铺野餐垫。",
      status: "archived",
      targetFragments: 8,
      currentFragments: 5,
      coverMediaId: "fixture-park-picnic",
      fragmentVisualMode: "grid_reveal",
      realizedCount: 0
    }
  ];
}

async function loadNotificationPreferences(familyId: string, accessToken?: string) {
  if (!accessToken) return [];
  try {
    return await coreGetJson<NotificationPreferenceResponse[]>(`/notification-preferences?familyId=${encodeURIComponent(familyId)}`, accessToken);
  } catch (error) {
    if (error instanceof Error && error.name === "CoreApiUnauthorizedError") throw new ParentWebUnauthorizedError();
    throw new ParentDashboardUnavailableError("数据加载失败，请稍后重试。");
  }
}

async function responseErrorSummary(response: Response) {
  await response.text().catch(() => "");
  return `服务暂时不可用（错误码 ${response.status}），请稍后重试。`;
}

function errorMessage(error: unknown) {
  return error instanceof Error && error.message ? "请求未完成，请稍后重试。" : "数据加载失败，请稍后重试。";
}

function compactErrorText(value: string) {
  const normalized = value.replace(/\s+/g, " ").trim();
  return normalized.length > 120 ? `${normalized.slice(0, 120)}...` : normalized;
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
