import { child, currentWish, dashboardMetrics, memories, pendingReviews, roomState, todayTasks, weeklyPlan } from "@wishpool/app-fixtures";
import { getApiRuntimeConfig } from "@/lib/api";

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
  coreApiHealthy: boolean;
};

export async function loadParentDashboardData(): Promise<ParentDashboardData> {
  const config = getApiRuntimeConfig();
  const fixture = fixtureDashboardData(false);
  if (process.env.WISHPOOL_PARENT_USE_API !== "true") return fixture;

  try {
    const health = await fetch(`${config.coreApiBaseUrl}/actuator/health`, {
      cache: "no-store"
    });
    return {
      ...fixture,
      source: "api",
      coreApiHealthy: health.ok
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
    coreApiHealthy
  };
}
