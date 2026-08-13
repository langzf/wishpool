import { dashboardMetrics, family, pendingReviews, privacyQueue } from "@wishpool/app-fixtures";
import { buildAdminHeaders, getAdminRuntimeConfig } from "@/lib/runtime";
import { configForAdminWebSession, type AdminWebSession } from "@/lib/session";

export type AdminServiceHealth = {
  name: string;
  status: string;
  latency: string;
  entry?: string;
};

export type AdminQueueRow = {
  name: string;
  pending: number;
  retrying: number;
};

export type AdminAuditRow = {
  id: string;
  actor: string;
  action: string;
  target: string;
  time: string;
};

export type AdminDashboardData = {
  source: "admin-api" | "fixture";
  familyName: string;
  activeChildren: number;
  pendingReviews: number;
  migrationCount: number;
  serviceHealth: AdminServiceHealth[];
  queues: AdminQueueRow[];
  families: AdminFamilyRow[];
  privacyQueue: typeof privacyQueue;
  auditRows: AdminAuditRow[];
};

export async function loadAdminDashboardData(session?: AdminWebSession | null): Promise<AdminDashboardData> {
  const config = configForAdminWebSession(session);
  const fixture = fixtureAdminDashboardData();
  if (!config.adminToken) return fixture;

  try {
    const [health, dashboard, families, privacyRequests, auditLogs] = await Promise.all([
      fetch(`${config.adminApiBaseUrl}/health`, { cache: "no-store" }),
      fetch(`${config.adminApiBaseUrl}/admin/dashboard`, {
        cache: "no-store",
        headers: buildAdminHeaders(config.adminToken)
      }),
      fetch(`${config.adminApiBaseUrl}/admin/families?limit=10`, {
        cache: "no-store",
        headers: buildAdminHeaders(config.adminToken)
      }),
      fetch(`${config.adminApiBaseUrl}/admin/privacy-requests?limit=10`, {
        cache: "no-store",
        headers: buildAdminHeaders(config.adminToken)
      }),
      fetch(`${config.adminApiBaseUrl}/admin/audit-logs?limit=12`, {
        cache: "no-store",
        headers: buildAdminHeaders(config.adminToken)
      })
    ]);
    if (!dashboard.ok) return fixture;
    const metrics = (await dashboard.json()) as AdminDashboardResponse;
    const familyRows = families.ok ? ((await families.json()) as AdminFamilyResponse[]) : [];
    const privacyRows = privacyRequests.ok ? ((await privacyRequests.json()) as AdminPrivacyRequestResponse[]) : [];
    const auditRows = auditLogs.ok ? ((await auditLogs.json()) as AdminAuditLogResponse[]) : [];
    return {
      ...fixture,
      source: "admin-api",
      activeChildren: metrics.activeChildCount,
      pendingReviews: metrics.pendingReviewCount,
      queues: [
        { name: "outbox.publish", pending: metrics.pendingOutboxCount, retrying: 0 },
        { name: "media.processing", pending: metrics.processingMediaCount, retrying: 0 },
        { name: "ai.precheck", pending: metrics.runningAiJobCount, retrying: 0 },
        { name: "notification.dispatch", pending: metrics.pendingNotificationCount, retrying: 0 },
        { name: "privacy.deletion", pending: metrics.openPrivacyRequestCount, retrying: 0 }
      ],
      families: familyRows.map((row) => ({
        id: row.id,
        name: row.name,
        timezone: row.timezone,
        status: row.status,
        childCount: row.childCount,
        memberCount: row.memberCount,
        createdAt: row.createdAt
      })),
      privacyQueue: privacyRows.map((request) => ({
        id: request.id,
        type: request.requestType,
        requesterName: request.requestedBy,
        status: request.status,
        createdAt: request.createdAt
      })),
      auditRows: auditRows.map((row) => ({
        id: row.id,
        actor: row.actorUserId ?? row.actorRole,
        action: row.action,
        target: `${row.resourceType}${row.resourceId ? `/${row.resourceId}` : ""}`,
        time: row.createdAt
      })),
      serviceHealth: fixture.serviceHealth.map((service) =>
        service.name === "Admin API"
          ? { ...service, status: health.ok ? "healthy" : "degraded", entry: config.adminApiBaseUrl }
          : service
      )
    };
  } catch {
    return fixture;
  }
}

function fixtureAdminDashboardData(): AdminDashboardData {
  return {
    source: "fixture",
    familyName: family.name,
    activeChildren: dashboardMetrics.activeChildren,
    pendingReviews: pendingReviews.length,
    migrationCount: 9,
    serviceHealth: [
      { name: "Core API", status: "healthy", latency: "34 ms" },
      { name: "Realtime Gateway", status: "healthy", latency: "21 ms" },
      { name: "Workflow Worker", status: "healthy", latency: "outbox active" },
      { name: "Media Worker", status: "healthy", latency: "ready 94%" },
      { name: "AI Worker", status: "configured", latency: "local provider" },
      { name: "Notification Service", status: "healthy", latency: "inbox dispatcher" },
      { name: "Admin API", status: "healthy", latency: "local governance" }
    ],
    queues: [
      { name: "outbox.publish", pending: 0, retrying: 0 },
      { name: "media.processing", pending: 1, retrying: 0 },
      { name: "ai.precheck", pending: pendingReviews.length, retrying: 0 },
      { name: "notification.dispatch", pending: 0, retrying: 0 },
      { name: "privacy.deletion", pending: privacyQueue.length, retrying: 0 }
    ],
    families: [
      {
        id: family.id,
        name: family.name,
        timezone: family.timezone,
        status: "active",
        childCount: 1,
        memberCount: family.members.length,
        createdAt: "2026-08-12T09:00:00+08:00"
      }
    ],
    privacyQueue,
    auditRows: [
      { id: "audit-1", actor: "parent-1", action: "review.approved", target: "submission-audio", time: "19:18" },
      { id: "audit-2", actor: "worker-media", action: "media.processing_completed", target: "media-reading", time: "19:12" },
      { id: "audit-3", actor: "system", action: "planning.weekly_plan_saved", target: "plan-week-33", time: "周日" }
    ]
  };
}

type AdminDashboardResponse = {
  activeChildCount: number;
  pendingReviewCount: number;
  pendingNotificationCount: number;
  pendingOutboxCount: number;
  processingMediaCount: number;
  runningAiJobCount: number;
  openPrivacyRequestCount: number;
};

export type AdminFamilyRow = {
  id: string;
  name: string;
  timezone: string;
  status: string;
  childCount: number;
  memberCount: number;
  createdAt: string;
};

type AdminFamilyResponse = AdminFamilyRow;

type AdminPrivacyRequestResponse = {
  id: string;
  requestType: string;
  status: string;
  requestedBy: string;
  createdAt: string;
};

type AdminAuditLogResponse = {
  id: string;
  actorUserId?: string | null;
  actorRole: string;
  action: string;
  resourceType: string;
  resourceId?: string | null;
  createdAt: string;
};
