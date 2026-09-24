export type ApiRuntimeConfig = {
  coreApiBaseUrl: string;
  realtimeBaseUrl: string;
  demoMode: boolean;
  accessToken?: string;
  familyId?: string;
  childId?: string;
};

export function getApiRuntimeConfig(): ApiRuntimeConfig {
  return {
    coreApiBaseUrl: process.env.NEXT_PUBLIC_WISHPOOL_CORE_API_URL ?? "http://localhost:8080",
    realtimeBaseUrl: process.env.NEXT_PUBLIC_WISHPOOL_REALTIME_URL ?? "http://localhost:8090",
    demoMode: process.env.WISHPOOL_PARENT_WEB_DEMO === "1",
    accessToken: process.env.WISHPOOL_PARENT_ACCESS_TOKEN,
    familyId: process.env.WISHPOOL_PARENT_FAMILY_ID,
    childId: process.env.WISHPOOL_PARENT_CHILD_ID
  };
}

export function buildAuthHeaders(accessToken?: string): HeadersInit {
  return accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
}
