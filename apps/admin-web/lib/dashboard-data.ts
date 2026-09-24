import { dashboardMetrics, family, pendingReviews, privacyQueue } from "@wishpool/app-fixtures";
import { buildAdminHeaders } from "@/lib/runtime";
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

export type AdminFamilyRow = {
  id: string;
  name: string;
  timezone: string;
  status: string;
  childCount: number;
  memberCount: number;
  createdAt: string;
};

export type AdminPrivacyQueueRow = {
  id: string;
  type: string;
  requesterName: string;
  status: string;
  createdAt: string;
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
  privacyQueue: AdminPrivacyQueueRow[];
  auditRows: AdminAuditRow[];
};

export class AdminWebUnauthorizedError extends Error {
  constructor() {
    super("Admin web session is no longer authorized.");
    this.name = "AdminWebUnauthorizedError";
  }
}

export function isAdminWebUnauthorizedError(error: unknown): error is AdminWebUnauthorizedError {
  return error instanceof AdminWebUnauthorizedError;
}

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
    if ([dashboard, families, privacyRequests, auditLogs].some((response) => response.status === 401)) {
      throw new AdminWebUnauthorizedError();
    }
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
      families: mapAdminFamilies(familyRows),
      privacyQueue: mapAdminPrivacyRequests(privacyRows),
      auditRows: mapAdminAuditLogs(auditRows),
      serviceHealth: fixture.serviceHealth.map((service) =>
        service.name === "Admin API"
          ? { ...service, status: health.ok ? "healthy" : "degraded", entry: config.adminApiBaseUrl }
          : service
      )
    };
  } catch (error) {
    if (isAdminWebUnauthorizedError(error)) throw error;
    return fixture;
  }
}

export async function loadAdminFamiliesData(
  session?: AdminWebSession | null
): Promise<Pick<AdminDashboardData, "source" | "families">> {
  const config = configForAdminWebSession(session);
  const fixture = fixtureAdminDashboardData();
  if (!config.adminToken) return { source: fixture.source, families: fixture.families };

  try {
    const response = await fetch(`${config.adminApiBaseUrl}/admin/families?limit=100`, {
      cache: "no-store",
      headers: buildAdminHeaders(config.adminToken)
    });
    if (response.status === 401) throw new AdminWebUnauthorizedError();
    if (!response.ok) return { source: fixture.source, families: fixture.families };
    const rows = (await response.json()) as AdminFamilyResponse[];
    return { source: "admin-api", families: mapAdminFamilies(rows) };
  } catch (error) {
    if (isAdminWebUnauthorizedError(error)) throw error;
    return { source: fixture.source, families: fixture.families };
  }
}

export async function loadAdminPrivacyData(
  session?: AdminWebSession | null
): Promise<Pick<AdminDashboardData, "source" | "privacyQueue">> {
  const config = configForAdminWebSession(session);
  const fixture = fixtureAdminDashboardData();
  if (!config.adminToken) return { source: fixture.source, privacyQueue: fixture.privacyQueue };

  try {
    const response = await fetch(`${config.adminApiBaseUrl}/admin/privacy-requests?limit=100`, {
      cache: "no-store",
      headers: buildAdminHeaders(config.adminToken)
    });
    if (response.status === 401) throw new AdminWebUnauthorizedError();
    if (!response.ok) return { source: fixture.source, privacyQueue: fixture.privacyQueue };
    const rows = (await response.json()) as AdminPrivacyRequestResponse[];
    return { source: "admin-api", privacyQueue: mapAdminPrivacyRequests(rows) };
  } catch (error) {
    if (isAdminWebUnauthorizedError(error)) throw error;
    return { source: fixture.source, privacyQueue: fixture.privacyQueue };
  }
}

export async function loadAdminAuditData(
  session?: AdminWebSession | null
): Promise<Pick<AdminDashboardData, "source" | "auditRows">> {
  const config = configForAdminWebSession(session);
  const fixture = fixtureAdminDashboardData();
  if (!config.adminToken) return { source: fixture.source, auditRows: fixture.auditRows };

  try {
    const response = await fetch(`${config.adminApiBaseUrl}/admin/audit-logs?limit=100`, {
      cache: "no-store",
      headers: buildAdminHeaders(config.adminToken)
    });
    if (response.status === 401) throw new AdminWebUnauthorizedError();
    if (!response.ok) return { source: fixture.source, auditRows: fixture.auditRows };
    const rows = (await response.json()) as AdminAuditLogResponse[];
    return { source: "admin-api", auditRows: mapAdminAuditLogs(rows) };
  } catch (error) {
    if (isAdminWebUnauthorizedError(error)) throw error;
    return { source: fixture.source, auditRows: fixture.auditRows };
  }
}

export function loadAdminQueuesData(data: AdminDashboardData): Pick<AdminDashboardData, "source" | "queues"> {
  return { source: data.source, queues: data.queues };
}

export function loadAdminStorageData(
  data: AdminDashboardData
): Pick<AdminDashboardData, "source" | "serviceHealth"> {
  return { source: data.source, serviceHealth: data.serviceHealth };
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

function mapAdminFamilies(rows: AdminFamilyResponse[]): AdminFamilyRow[] {
  return rows.map((row) => ({
    id: row.id,
    name: row.name,
    timezone: row.timezone,
    status: row.status,
    childCount: row.childCount,
    memberCount: row.memberCount,
    createdAt: row.createdAt
  }));
}

function mapAdminPrivacyRequests(rows: AdminPrivacyRequestResponse[]): AdminPrivacyQueueRow[] {
  return rows.map((request) => ({
    id: request.id,
    type: request.requestType,
    requesterName: request.requestedBy,
    status: request.status,
    createdAt: request.createdAt
  }));
}

function mapAdminAuditLogs(rows: AdminAuditLogResponse[]): AdminAuditRow[] {
  return rows.map((row) => ({
    id: row.id,
    actor: row.actorUserId ?? row.actorRole,
    action: row.action,
    target: `${row.resourceType}${row.resourceId ? `/${row.resourceId}` : ""}`,
    time: row.createdAt
  }));
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
