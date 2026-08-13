import { cookies } from "next/headers";
import { getAdminRuntimeConfig, type AdminRuntimeConfig } from "@/lib/runtime";

const adminTokenCookie = "wp_admin_token";
const maxAge = 60 * 60 * 24 * 30;

export type AdminWebSession = {
  adminToken: string;
};

export async function getAdminWebSession(): Promise<AdminWebSession | null> {
  const store = await cookies();
  const token = store.get(adminTokenCookie)?.value ?? getAdminRuntimeConfig().adminToken;
  return token ? { adminToken: token } : null;
}

export async function saveAdminWebSession(session: AdminWebSession) {
  const store = await cookies();
  store.set(adminTokenCookie, session.adminToken, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge
  });
}

export async function clearAdminWebSession() {
  const store = await cookies();
  store.delete(adminTokenCookie);
}

export function configForAdminWebSession(session: AdminWebSession | null | undefined): AdminRuntimeConfig {
  const config = getAdminRuntimeConfig();
  return {
    ...config,
    adminToken: session?.adminToken ?? config.adminToken
  };
}
