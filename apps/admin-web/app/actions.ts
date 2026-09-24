"use server";

import { redirect } from "next/navigation";
import { isRedirectError } from "next/dist/client/components/redirect-error";
import { buildAdminHeaders, getAdminRuntimeConfig } from "@/lib/runtime";
import { clearAdminWebSession, getAdminWebSession, saveAdminWebSession } from "@/lib/session";

export async function loginAdminAction(formData: FormData) {
  const token = valueFromForm(formData, "adminToken");
  if (!token) redirect(`/?authError=${encodeURIComponent("请输入管理令牌")}`);
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
    if (isRedirectError(error)) throw error;
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
    `/storage?mediaAccessUrl=${encodeURIComponent(grant.accessUrl ?? "")}` +
      `&mediaAccessExpiresAt=${encodeURIComponent(grant.expiresAt ?? "")}` +
      `&mediaAccessAuditLogId=${encodeURIComponent(grant.auditLogId ?? "")}#media-access`,
  );
}

export async function saveImageModelProviderAction(formData: FormData) {
  const id = valueFromForm(formData, "id");
  const code = valueFromForm(formData, "code");
  const displayName = valueFromForm(formData, "displayName");
  const providerType = valueFromForm(formData, "providerType");
  const baseUrl = valueFromForm(formData, "baseUrl");
  const modelName = valueFromForm(formData, "modelName");
  if (!displayName || !providerType || !baseUrl || !modelName || (!id && !code)) {
    redirect(`/ai-models?actionError=${encodeURIComponent("请补全模型编码、名称、类型、URL 和模型标识。")}`);
  }

  const payload = {
    code,
    displayName,
    providerType,
    baseUrl,
    apiKey: valueFromForm(formData, "apiKey") ?? undefined,
    modelName,
    extraParams: parseExtraParams(valueFromForm(formData, "extraParams")),
    isDefault: formData.get("isDefault") === "on",
    isEnabled: formData.get("isEnabled") !== null
  };
  const path = id ? `/admin/image-model-providers/${id}` : "/admin/image-model-providers";
  const method = id ? "PUT" : "POST";
  await writeAdminApi(path, method, payload);
  redirect(`/ai-models?actionSuccess=${encodeURIComponent("模型配置已保存。")}`);
}

export async function toggleImageModelProviderAction(formData: FormData) {
  const id = valueFromForm(formData, "id");
  const isEnabled = valueFromForm(formData, "isEnabled") === "true";
  if (!id) redirect(`/ai-models?actionError=${encodeURIComponent("缺少模型 ID。")}`);
  await writeAdminApi(`/admin/image-model-providers/${id}/toggle`, "POST", { isEnabled });
  redirect(`/ai-models?actionSuccess=${encodeURIComponent(isEnabled ? "模型已启用。" : "模型已停用。")}`);
}

export async function setDefaultImageModelProviderAction(formData: FormData) {
  const id = valueFromForm(formData, "id");
  if (!id) redirect(`/ai-models?actionError=${encodeURIComponent("缺少模型 ID。")}`);
  await writeAdminApi(`/admin/image-model-providers/${id}/set-default`, "POST", {});
  redirect(`/ai-models?actionSuccess=${encodeURIComponent("默认模型已更新。")}`);
}

export async function deleteImageModelProviderAction(formData: FormData) {
  const id = valueFromForm(formData, "id");
  if (!id) redirect(`/ai-models?actionError=${encodeURIComponent("缺少模型 ID。")}`);
  await writeAdminApi(`/admin/image-model-providers/${id}`, "DELETE", {});
  redirect(`/ai-models?actionSuccess=${encodeURIComponent("模型配置已删除。")}`);
}

export async function saveImageGenUsageAction(formData: FormData) {
  const usageCode = valueFromForm(formData, "usageCode");
  const providerCode = valueFromForm(formData, "providerCode");
  if (!usageCode || !providerCode) redirect(`/ai-models?actionError=${encodeURIComponent("请选择业务场景和模型。")}`);
  await writeAdminApi(`/admin/image-gen-usages/${usageCode}`, "PUT", { usageCode, providerCode });
  redirect(`/ai-models?actionSuccess=${encodeURIComponent("业务默认模型已保存。")}`);
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

async function writeAdminApi(path: string, method: "POST" | "PUT" | "DELETE", body: Record<string, unknown>) {
  const config = await requireAdminActionConfig();
  if (!config.adminToken) throw new Error("请先登录管理后台。");
  const response = await fetch(`${config.adminApiBaseUrl}${path}`, {
    method,
    cache: "no-store",
    headers: {
      ...buildAdminHeaders(config.adminToken),
      "Content-Type": "application/json"
    },
    body: method === "DELETE" ? undefined : JSON.stringify(body)
  });
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`Admin command failed: ${response.status} ${errorBody}`);
  }
}

function parseExtraParams(value: string | null): Record<string, unknown> {
  if (!value) return {};
  try {
    const parsed = JSON.parse(value);
    if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
      throw new Error("extra_params must be an object");
    }
    return parsed as Record<string, unknown>;
  } catch {
    throw new Error("extra_params 必须是合法 JSON 对象。");
  }
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : "操作失败，请稍后重试。";
}
