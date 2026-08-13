import { cookies } from "next/headers";
import { getApiRuntimeConfig, type ApiRuntimeConfig } from "@/lib/api";

const maxAge = 60 * 60 * 24 * 30;

const cookieNames = {
  accessToken: "wp_parent_access_token",
  refreshToken: "wp_parent_refresh_token",
  familyId: "wp_parent_family_id",
  childId: "wp_parent_child_id",
  userName: "wp_parent_user_name"
};

export type ParentWebSession = {
  accessToken: string;
  refreshToken?: string;
  familyId?: string;
  childId?: string;
  userName?: string;
};

export async function getParentWebSession(): Promise<ParentWebSession | null> {
  const store = await cookies();
  const config = getApiRuntimeConfig();
  const accessToken = store.get(cookieNames.accessToken)?.value ?? config.accessToken;
  if (!accessToken) return null;

  const userNameCookie = store.get(cookieNames.userName)?.value;
  return {
    accessToken,
    refreshToken: store.get(cookieNames.refreshToken)?.value,
    familyId: store.get(cookieNames.familyId)?.value ?? config.familyId,
    childId: store.get(cookieNames.childId)?.value ?? config.childId,
    userName: userNameCookie ? decodeURIComponent(userNameCookie) : undefined
  };
}

export async function saveParentWebSession(session: ParentWebSession) {
  const store = await cookies();
  const options = {
    httpOnly: true,
    sameSite: "lax" as const,
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge
  };

  store.set(cookieNames.accessToken, session.accessToken, options);
  setOptionalCookie(store, cookieNames.refreshToken, session.refreshToken, options);
  setOptionalCookie(store, cookieNames.familyId, session.familyId, options);
  setOptionalCookie(store, cookieNames.childId, session.childId, options);
  setOptionalCookie(store, cookieNames.userName, session.userName ? encodeURIComponent(session.userName) : undefined, options);
}

export async function updateParentWebSession(patch: Partial<ParentWebSession>) {
  const current = await getParentWebSession();
  if (!current) throw new Error("请先登录家长账号。");
  await saveParentWebSession({ ...current, ...patch });
}

export async function clearParentWebSession() {
  const store = await cookies();
  Object.values(cookieNames).forEach((name) => store.delete(name));
}

export function configForParentWebSession(session: ParentWebSession | null | undefined): ApiRuntimeConfig {
  const config = getApiRuntimeConfig();
  return {
    ...config,
    accessToken: session?.accessToken ?? config.accessToken,
    familyId: session?.familyId ?? config.familyId,
    childId: session?.childId ?? config.childId
  };
}

function setOptionalCookie(
  store: Awaited<ReturnType<typeof cookies>>,
  name: string,
  value: string | undefined,
  options: Parameters<typeof store.set>[2]
) {
  if (value && value.length > 0) {
    store.set(name, value, options);
  } else {
    store.delete(name);
  }
}
