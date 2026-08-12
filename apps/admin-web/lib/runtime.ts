export type AdminRuntimeConfig = {
  adminApiBaseUrl: string;
  coreApiBaseUrl: string;
  temporalUiUrl: string;
  minioConsoleUrl: string;
};

export function getAdminRuntimeConfig(): AdminRuntimeConfig {
  return {
    adminApiBaseUrl: process.env.NEXT_PUBLIC_WISHPOOL_ADMIN_API_URL ?? "http://localhost:8083",
    coreApiBaseUrl: process.env.NEXT_PUBLIC_WISHPOOL_CORE_API_URL ?? "http://localhost:8080",
    temporalUiUrl: process.env.NEXT_PUBLIC_WISHPOOL_TEMPORAL_UI_URL ?? "http://localhost:8088",
    minioConsoleUrl: process.env.NEXT_PUBLIC_WISHPOOL_MINIO_CONSOLE_URL ?? "http://localhost:9001"
  };
}
