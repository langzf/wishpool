import { dashboardMetrics, family, pendingReviews, privacyQueue } from "@wishpool/app-fixtures";
import { getAdminRuntimeConfig } from "@/lib/runtime";

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
  privacyQueue: typeof privacyQueue;
  auditRows: AdminAuditRow[];
};

export async function loadAdminDashboardData(): Promise<AdminDashboardData> {
  const config = getAdminRuntimeConfig();
  const fixture = fixtureAdminDashboardData();
  if (process.env.WISHPOOL_ADMIN_USE_API !== "true") return fixture;

  try {
    const health = await fetch(`${config.adminApiBaseUrl}/health`, {
      cache: "no-store"
    });
    return {
      ...fixture,
      source: "admin-api",
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
    privacyQueue,
    auditRows: [
      { id: "audit-1", actor: "parent-1", action: "review.approved", target: "submission-audio", time: "19:18" },
      { id: "audit-2", actor: "worker-media", action: "media.processing_completed", target: "media-reading", time: "19:12" },
      { id: "audit-3", actor: "system", action: "planning.weekly_plan_saved", target: "plan-week-33", time: "周日" }
    ]
  };
}
