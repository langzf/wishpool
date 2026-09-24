import { buildAuthHeaders, getApiRuntimeConfig } from "@/lib/api";
import { getParentWebSession, saveParentWebSession } from "@/lib/session";

export class CoreApiUnauthorizedError extends Error {
  constructor(message = "Core API authentication expired.") {
    super(message);
    this.name = "CoreApiUnauthorizedError";
  }
}

export function isCoreApiUnauthorizedError(error: unknown): error is CoreApiUnauthorizedError {
  return error instanceof CoreApiUnauthorizedError;
}

export async function coreGetJson<T>(path: string, accessToken?: string): Promise<T> {
  return coreRequestJson<T>("GET", path, undefined, accessToken);
}

export async function corePostJson<T>(
  path: string,
  body: Record<string, unknown>,
  accessToken?: string,
  idempotencyKey?: string
): Promise<T> {
  return coreRequestJson<T>("POST", path, body, accessToken, idempotencyKey);
}

export async function coreRequestJson<T>(
  method: "GET" | "POST" | "PUT" | "PATCH" | "DELETE",
  path: string,
  body?: Record<string, unknown>,
  accessToken?: string,
  idempotencyKey?: string
): Promise<T> {
  const config = getApiRuntimeConfig();
  const request = (token?: string) => fetch(`${config.coreApiBaseUrl}${path}`, {
    method,
    cache: "no-store",
    headers: {
      ...buildAuthHeaders(token),
      ...(body ? { "Content-Type": "application/json" } : {}),
      ...(idempotencyKey ? { "Idempotency-Key": idempotencyKey } : {})
    },
    ...(body ? { body: JSON.stringify(body) } : {})
  });
  const response = await request(accessToken);
  if (response.status !== 401 || path === "/auth/refresh") return parseCoreResponse<T>(response);
  // Page requests hand 401s to the auth-expired route, where cookies can be
  // updated safely before redirecting back to the original page.
  return parseCoreResponse<T>(response);
}

export type RefreshedAuthTokenPair = { accessToken: string; refreshToken: string; expiresInSec: number; user?: { displayName?: string }; primaryFamilyId?: string | null };
const refreshInFlight = new Map<string, Promise<RefreshedAuthTokenPair>>();

export function refreshParentSession(refreshToken: string): Promise<RefreshedAuthTokenPair> {
  const existing = refreshInFlight.get(refreshToken);
  if (existing) return existing;
  const promise = (async () => {
    const config = getApiRuntimeConfig();
    const response = await fetch(`${config.coreApiBaseUrl}/auth/refresh`, {
      method: "POST", cache: "no-store", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken })
    });
    if (!response.ok) throw new Error("refresh failed");
    const result = (await response.json()) as RefreshedAuthTokenPair;
    if (!result.accessToken || !result.refreshToken) throw new Error("invalid refresh response");
    const current = await getParentWebSession();
    if (current) await saveParentWebSession({ ...current, accessToken: result.accessToken, refreshToken: result.refreshToken, familyId: result.primaryFamilyId ?? current.familyId, userName: result.user?.displayName ?? current.userName });
    return result;
  })();
  refreshInFlight.set(refreshToken, promise);
  void promise.then(
    () => setTimeout(() => refreshInFlight.delete(refreshToken), 30_000),
    () => setTimeout(() => refreshInFlight.delete(refreshToken), 30_000)
  );
  return promise;
}

async function parseCoreResponse<T>(response: Response): Promise<T> {
  const text = await response.text();
  if (!response.ok) {
    if (response.status === 401) {
      throw new CoreApiUnauthorizedError(text || response.statusText);
    }
    throw new Error(`Core API ${response.status}: ${text || response.statusText}`);
  }
  return (text.length > 0 ? JSON.parse(text) : {}) as T;
}
