"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { buildAuthHeaders, getApiRuntimeConfig } from "@/lib/api";
import { coreGetJson, corePostJson } from "@/lib/core-client";
import { clearParentWebSession, getParentWebSession, saveParentWebSession, updateParentWebSession } from "@/lib/session";

type ReviewDecision = "approved" | "needs_revision";

type AuthTokenPair = {
  accessToken: string;
  refreshToken: string;
  expiresInSec: number;
  user: { displayName: string };
  primaryFamilyId?: string | null;
};

type PhoneCodeCreated = {
  verificationToken: string;
  expiresAt: string;
  debugCode?: string | null;
};

type FamilyResponse = {
  id: string;
  name: string;
  timezone: string;
  status: string;
};

type ChildProfileResponse = {
  id: string;
  familyId: string;
  nickname: string;
  roomTheme: string;
  status: string;
};

type FamilyContext = {
  family: FamilyResponse;
  member: {
    childId?: string | null;
  };
};

type MeResponse = {
  families: FamilyContext[];
};

export async function requestParentPhoneCodeAction(formData: FormData) {
  const phoneNumber = valueFromForm(formData, "phoneNumber");
  if (!phoneNumber) redirect("/?authError=请输入手机号");
  try {
    const result = await corePostJson<PhoneCodeCreated>("/auth/phone-codes", { phoneNumber, purpose: "login" });
    redirect(
      `/?phone=${encodeURIComponent(phoneNumber)}&verificationToken=${encodeURIComponent(result.verificationToken)}&debugCode=${encodeURIComponent(
        result.debugCode ?? ""
      )}`
    );
  } catch (error) {
    redirect(`/?authError=${encodeURIComponent(errorMessage(error))}`);
  }
}

export async function loginParentAction(formData: FormData) {
  const verificationToken = valueFromForm(formData, "verificationToken");
  const code = valueFromForm(formData, "code");
  if (!verificationToken || !code) redirect("/?authError=请输入验证码");

  try {
    const auth = await corePostJson<AuthTokenPair>("/auth/login", {
      provider: "phone",
      credential: `${verificationToken}:${code}`,
      device: {
        platform: "web",
        deviceName: "WishPool Parent Web"
      }
    });
    const selection = await selectDefaultContext(auth.accessToken, auth.primaryFamilyId ?? undefined);
    await saveParentWebSession({
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
      familyId: selection.familyId,
      childId: selection.childId,
      userName: auth.user.displayName
    });
    redirect(selection.familyId && selection.childId ? "/" : "/?setup=1");
  } catch (error) {
    redirect(`/?authError=${encodeURIComponent(errorMessage(error))}`);
  }
}

export async function selectParentChildAction(formData: FormData) {
  const familyId = valueFromForm(formData, "familyId");
  const childId = valueFromForm(formData, "childId");
  if (!familyId || !childId) redirect("/?setup=1&setupError=请选择家庭和孩子");
  await updateParentWebSession({ familyId, childId });
  redirect("/");
}

export async function completeParentFamilySetupAction(formData: FormData) {
  const session = await requireParentSession();
  const familyName = valueFromForm(formData, "familyName");
  const timezone = valueFromForm(formData, "timezone") ?? "Asia/Shanghai";
  const childNickname = valueFromForm(formData, "childNickname");
  if (!familyName || !childNickname) redirect("/?setup=1&setupError=请补全家庭名称和孩子昵称");

  const birthYearValue = valueFromForm(formData, "birthYear");
  const childPayload = {
    nickname: childNickname,
    birthYear: birthYearValue ? Number(birthYearValue) : undefined,
    roomTheme: valueFromForm(formData, "roomTheme") ?? "forest"
  };

  const family = session.familyId
    ? { id: session.familyId }
    : await corePostJson<FamilyResponse>("/families", {
        name: familyName,
        timezone
      }, session.accessToken);

  const child = await corePostJson<ChildProfileResponse>(`/families/${family.id}/children`, childPayload, session.accessToken);
  await updateParentWebSession({ familyId: family.id, childId: child.id });
  redirect("/");
}

export async function logoutParentAction() {
  await clearParentWebSession();
  redirect("/");
}

export async function approveReviewAction(formData: FormData) {
  await submitReview(formData, "approved");
}

export async function requestRevisionAction(formData: FormData) {
  await submitReview(formData, "needs_revision");
}

export async function skipTaskAction(formData: FormData) {
  const taskId = valueFromForm(formData, "taskId");
  if (!taskId) return;
  await postCore(`/tasks/${taskId}/skip`, {
    reason: valueFromForm(formData, "reason") ?? "家庭临时调整"
  });
  revalidatePath("/");
}

export async function postponeTaskAction(formData: FormData) {
  const taskId = valueFromForm(formData, "taskId");
  const newDate = valueFromForm(formData, "newDate");
  if (!taskId || !newDate) return;
  await postCore(`/tasks/${taskId}/postpone`, {
    newDate,
    reason: valueFromForm(formData, "reason") ?? "顺延到下一天"
  });
  revalidatePath("/");
}

export async function saveWeeklyPlanAction(formData: FormData) {
  const rulesJson = valueFromForm(formData, "rulesJson");
  const childId = valueFromForm(formData, "childId");
  const weekId = valueFromForm(formData, "weekId");
  const startDate = valueFromForm(formData, "startDate");
  const endDate = valueFromForm(formData, "endDate");
  if (!rulesJson || !childId || !weekId || !startDate || !endDate) return;
  const config = await requireParentActionConfig();
  if (!config.familyId) throw new Error("请选择家庭后再保存周计划。");
  const rules = JSON.parse(rulesJson);
  await postCore("/plans", {
    familyId: config.familyId,
    childId,
    weekId,
    startDate,
    endDate,
    rewardMode: valueFromForm(formData, "rewardMode") ?? "flexible",
    rules
  });
  revalidatePath("/");
}

export async function createPairingSessionAction(formData: FormData) {
  const childId = valueFromForm(formData, "childId");
  if (!childId) return;
  const config = await requireParentActionConfig();
  if (!config.familyId) throw new Error("请选择家庭后再生成配对码。");
  const session = await postCore(`/families/${config.familyId}/pairing-sessions`, { childId }, { idempotent: false });
  const pairingCode = typeof session.pairingCode === "string" ? session.pairingCode : "";
  const expiresAt = typeof session.expiresAt === "string" ? session.expiresAt : "";
  redirect(`/?pairingCode=${encodeURIComponent(pairingCode)}&pairingExpiresAt=${encodeURIComponent(expiresAt)}#settings`);
}

export async function createWishAction(formData: FormData) {
  const childId = valueFromForm(formData, "childId");
  const title = valueFromForm(formData, "title");
  const weekId = valueFromForm(formData, "weekId");
  if (!childId || !title || !weekId) return;
  const config = await requireParentActionConfig();
  if (!config.familyId) throw new Error("请选择家庭后再创建心愿。");
  const wish = await postCore("/wishes", {
    familyId: config.familyId,
    childId,
    weekId,
    title,
    note: valueFromForm(formData, "note"),
    requiredFragments: Number(valueFromForm(formData, "requiredFragments") ?? "10"),
    rewardMode: valueFromForm(formData, "rewardMode") ?? "flexible"
  });
  const wishId = typeof wish.id === "string" ? wish.id : "";
  if (wishId) {
    await postCore(`/wishes/${wishId}/activate`, {});
  }
  revalidatePath("/");
}

export async function exportMemoryAction(formData: FormData) {
  const memoryId = valueFromForm(formData, "memoryId");
  if (!memoryId) return;
  await postCore(`/memories/${memoryId}/export`, {
    format: valueFromForm(formData, "format") ?? "pdf"
  });
  revalidatePath("/");
}

export async function arrangeRoomItemAction(formData: FormData) {
  const itemId = valueFromForm(formData, "itemId");
  if (!itemId) return;
  await postCore(`/room/items/${itemId}/arrange`, {
    position: {
      x: Number(valueFromForm(formData, "x") ?? "16"),
      y: Number(valueFromForm(formData, "y") ?? "32")
    }
  });
  revalidatePath("/");
}

export async function markNotificationsReadAction(formData: FormData) {
  const ids = formData.getAll("notificationIds").filter((value): value is string => typeof value === "string" && value.length > 0);
  if (ids.length === 0) return;
  await postCore("/notifications/read", {
    notificationIds: ids
  }, { idempotent: false });
  revalidatePath("/");
}

export async function updateNotificationPreferenceAction(formData: FormData) {
  const notificationType = valueFromForm(formData, "notificationType");
  if (!notificationType) return;
  const config = await requireParentActionConfig();
  if (!config.familyId) throw new Error("请选择家庭后再更新通知偏好。");
  await putCore("/notification-preferences", {
    familyId: config.familyId,
    notificationType,
    enabled: valueFromForm(formData, "enabled") === "true",
    channels: {
      inbox: true,
      push: false
    }
  });
  revalidatePath("/");
}

async function submitReview(formData: FormData, decision: ReviewDecision) {
  const submissionId = valueFromForm(formData, "submissionId");
  if (!submissionId) return;

  const config = await requireParentActionConfig();
  if (!config.accessToken) {
    throw new Error("请先登录家长账号。");
  }

  const feedbackText =
    valueFromForm(formData, "feedbackText") ??
    (decision === "approved" ? "看到了，完成得很好。" : "请再补充一点，让记录更完整。");
  const response = await fetch(`${config.coreApiBaseUrl}/reviews`, {
    method: "POST",
    cache: "no-store",
    headers: {
      ...buildAuthHeaders(config.accessToken),
      "Content-Type": "application/json",
      "Idempotency-Key": `parent-web-${decision}-${submissionId}`
    },
    body: JSON.stringify({
      submissionId,
      decision,
      feedback: {
        emoji: decision === "approved" ? "heart" : "seed",
        text: feedbackText
      }
    })
  });

  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`Review submission failed: ${response.status} ${errorBody}`);
  }

  revalidatePath("/");
}

async function postCore(path: string, body: Record<string, unknown>, options: { idempotent?: boolean } = {}) {
  return writeCore("POST", path, body, options);
}

async function putCore(path: string, body: Record<string, unknown>) {
  return writeCore("PUT", path, body, { idempotent: false });
}

async function writeCore(method: "POST" | "PUT", path: string, body: Record<string, unknown>, options: { idempotent?: boolean } = {}) {
  const config = await requireParentActionConfig();
  if (!config.accessToken) {
    throw new Error("请先登录家长账号。");
  }
  const response = await fetch(`${config.coreApiBaseUrl}${path}`, {
    method,
    cache: "no-store",
    headers: {
      ...buildAuthHeaders(config.accessToken),
      "Content-Type": "application/json",
      ...(options.idempotent === false ? {} : { "Idempotency-Key": `parent-web-${path.replace(/[^a-zA-Z0-9]/g, "-")}-${Date.now()}` })
    },
    body: JSON.stringify(body)
  });
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`Core command failed: ${response.status} ${errorBody}`);
  }
  const text = await response.text();
  return text.length > 0 ? (JSON.parse(text) as Record<string, unknown>) : {};
}

function valueFromForm(formData: FormData, key: string): string | null {
  const value = formData.get(key);
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : null;
}

async function requireParentSession() {
  const session = await getParentWebSession();
  if (!session?.accessToken) throw new Error("请先登录家长账号。");
  return session;
}

async function requireParentActionConfig() {
  const session = await getParentWebSession();
  const config = getApiRuntimeConfig();
  return {
    ...config,
    accessToken: session?.accessToken ?? config.accessToken,
    familyId: session?.familyId ?? config.familyId,
    childId: session?.childId ?? config.childId
  };
}

async function selectDefaultContext(accessToken: string, preferredFamilyId?: string) {
  const me = await coreGetJson<MeResponse>("/me", accessToken);
  const family = me.families.find((context) => context.family.id === preferredFamilyId) ?? me.families[0];
  if (!family) return {};

  const memberChildId = family.member.childId ?? undefined;
  const children = await coreGetJson<ChildProfileResponse[]>(`/families/${family.family.id}/children`, accessToken);
  return {
    familyId: family.family.id,
    childId: memberChildId ?? children.find((childProfile) => childProfile.status === "active")?.id ?? children[0]?.id
  };
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : "操作失败，请稍后重试。";
}
