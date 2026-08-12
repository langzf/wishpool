export type ApiRuntimeConfig = {
  coreApiBaseUrl: string;
  realtimeBaseUrl: string;
};

export function getApiRuntimeConfig(): ApiRuntimeConfig {
  return {
    coreApiBaseUrl: process.env.NEXT_PUBLIC_WISHPOOL_CORE_API_URL ?? "http://localhost:8080",
    realtimeBaseUrl: process.env.NEXT_PUBLIC_WISHPOOL_REALTIME_URL ?? "http://localhost:8090"
  };
}

export function buildAuthHeaders(accessToken?: string): HeadersInit {
  return accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
}
