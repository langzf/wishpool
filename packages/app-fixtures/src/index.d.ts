export type FamilyFixture = {
  id: string;
  name: string;
  timezone: string;
  members: Array<{ id: string; role: string; displayName: string }>;
};

export type ChildFixture = {
  id: string;
  familyId: string;
  nickname: string;
  age: number;
  roomTheme: string;
  status: string;
  starlightBalance: number;
  wishFragmentBalance: number;
};

export type TaskFixture = {
  id: string;
  title: string;
  category: string;
  submissionType: string;
  status: string;
  scheduledDate: string;
  isCore: boolean;
  requireReview: boolean;
  rewardStarlight: number;
};

export type WishFixture = {
  id: string;
  childId: string;
  title: string;
  description: string;
  status: string;
  targetFragments: number;
  currentFragments: number;
  coverMediaId: string;
};

export type PendingReviewFixture = {
  submissionId: string;
  childName: string;
  taskTitle: string;
  submittedAt: string;
  aiSummary: string;
  riskLevel: string;
  mediaType: string;
};

export type WeeklyPlanFixture = {
  id: string;
  childId: string;
  weekId: string;
  weekStartDate: string;
  weekEndDate: string;
  rewardMode: string;
  status: string;
  rules: Array<{
    title: string;
    category: string;
    submissionType: string;
    weekdays: number[];
    isCore: boolean;
    requireReview: boolean;
    rewardStarlight: number;
  }>;
};

export type MemoryFixture = {
  id: string;
  title: string;
  weekStartDate: string;
  summary: string;
  highlights: string[];
};

export type RoomStateFixture = {
  childId: string;
  theme: string;
  items: Array<{ id: string; kind: string; name: string; x: number; y: number; unlocked: boolean }>;
};

export type PrivacyRequestFixture = {
  id: string;
  type: string;
  requesterName: string;
  status: string;
  createdAt: string;
};

export type NotificationFixture = {
  id: string;
  type: string;
  title: string;
  body: string;
  status: string;
  createdAt: string;
};

export type NotificationPreferenceFixture = {
  notificationType: string;
  enabled: boolean;
  channels: Record<string, boolean>;
};

export type DashboardMetricsFixture = {
  activeChildren: number;
  pendingReviews: number;
  weeklyCompletionRate: number;
  starlightIssuedThisWeek: number;
  mediaProcessingReadyRate: number;
};

export type FixtureSnapshot = {
  family: FamilyFixture;
  child: ChildFixture;
  todayTasks: TaskFixture[];
  currentWish: WishFixture;
  pendingReviews: PendingReviewFixture[];
  weeklyPlan: WeeklyPlanFixture;
  memories: MemoryFixture[];
  roomState: RoomStateFixture;
  privacyQueue: PrivacyRequestFixture[];
  notificationInbox: NotificationFixture[];
  notificationPreferences: NotificationPreferenceFixture[];
  dashboardMetrics: DashboardMetricsFixture;
};

export declare const family: FamilyFixture;
export declare const child: ChildFixture;
export declare const todayTasks: TaskFixture[];
export declare const currentWish: WishFixture;
export declare const pendingReviews: PendingReviewFixture[];
export declare const weeklyPlan: WeeklyPlanFixture;
export declare const memories: MemoryFixture[];
export declare const roomState: RoomStateFixture;
export declare const privacyQueue: PrivacyRequestFixture[];
export declare const notificationInbox: NotificationFixture[];
export declare const notificationPreferences: NotificationPreferenceFixture[];
export declare const dashboardMetrics: DashboardMetricsFixture;
export declare function getFixtureSnapshot(): FixtureSnapshot;
