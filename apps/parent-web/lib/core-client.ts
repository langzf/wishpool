import { buildAuthHeaders, getApiRuntimeConfig } from "@/lib/api";

export async function coreGetJson<T>(path: string, accessToken?: string): Promise<T> {
  const config = getApiRuntimeConfig();
  const response = await fetch(`${config.coreApiBaseUrl}${path}`, {
    cache: "no-store",
    headers: buildAuthHeaders(accessToken)
  });
  return parseCoreResponse<T>(response);
}

export async function corePostJson<T>(
  path: string,
  body: Record<string, unknown>,
  accessToken?: string,
  idempotencyKey?: string
): Promise<T> {
  const config = getApiRuntimeConfig();
  const response = await fetch(`${config.coreApiBaseUrl}${path}`, {
    method: "POST",
    cache: "no-store",
    headers: {
      ...buildAuthHeaders(accessToken),
      "Content-Type": "application/json",
      ...(idempotencyKey ? { "Idempotency-Key": idempotencyKey } : {})
    },
    body: JSON.stringify(body)
  });
  return parseCoreResponse<T>(response);
}

async function parseCoreResponse<T>(response: Response): Promise<T> {
  const text = await response.text();
  if (!response.ok) {
    throw new Error(`Core API ${response.status}: ${text || response.statusText}`);
  }
  return (text.length > 0 ? JSON.parse(text) : {}) as T;
}
