"use server";

import { redirect } from "next/navigation";
import { buildAdminHeaders, getAdminRuntimeConfig } from "@/lib/runtime";
import { clearAdminWebSession, getAdminWebSession, saveAdminWebSession } from "@/lib/session";

export async function loginAdminAction(formData: FormData) {
  const token = valueFromForm(formData, "adminToken");
  if (!token) redirect("/?authError=请输入管理令牌");
  const config = getAdminRuntimeConfig();
  try {
    const response = await fetch(`${config.adminApiBaseUrl}/admin/dashboard`, {
      cache: "no-store",
      headers: buildAdminHeaders(token)
    });
    if (!response.ok) {
      const errorBody = await response.text();
      throw new Error(`${response.status} ${errorBody}`);
    }
    await saveAdminWebSession({ adminToken: token });
    redirect("/");
  } catch (error) {
    redirect(`/?authError=${encodeURIComponent(errorMessage(error))}`);
  }
}

export async function logoutAdminAction() {
  await clearAdminWebSession();
  redirect("/");
}

export async function grantMediaAccessAction(formData: FormData) {
  const familyId = valueFromForm(formData, "familyId");
  const mediaAssetId = valueFromForm(formData, "mediaAssetId");
  const reason = valueFromForm(formData, "reason");
  if (!familyId || !mediaAssetId || !reason) return;
  const config = await requireAdminActionConfig();
  if (!config.adminToken) throw new Error("请先登录管理后台。");

  const response = await fetch(`${config.adminApiBaseUrl}/admin/media-access-grants`, {
    method: "POST",
    cache: "no-store",
    headers: {
      ...buildAdminHeaders(config.adminToken),
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      familyId,
      mediaAssetId,
      reason,
      expiresInMinutes: Number(valueFromForm(formData, "expiresInMinutes") ?? "30")
    })
  });
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`Media access grant failed: ${response.status} ${errorBody}`);
  }
  const grant = (await response.json()) as { accessUrl?: string; expiresAt?: string; auditLogId?: string };
  redirect(
    `/?mediaAccessUrl=${encodeURIComponent(grant.accessUrl ?? "")}` +
      `&mediaAccessExpiresAt=${encodeURIComponent(grant.expiresAt ?? "")}` +
      `&mediaAccessAuditLogId=${encodeURIComponent(grant.auditLogId ?? "")}#media-access`,
  );
}

function valueFromForm(formData: FormData, key: string): string | null {
  const value = formData.get(key);
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : null;
}

async function requireAdminActionConfig() {
  const session = await getAdminWebSession();
  const config = getAdminRuntimeConfig();
  return {
    ...config,
    adminToken: session?.adminToken ?? config.adminToken
  };
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : "操作失败，请稍后重试。";
}
