"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { isRedirectError } from "next/dist/client/components/redirect-error";
import { buildAuthHeaders, getApiRuntimeConfig } from "@/lib/api";
import { coreGetJson, corePostJson, coreRequestJson } from "@/lib/core-client";
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

type MediaAssetResponse = {
  id: string;
  familyId: string;
  childId?: string | null;
  purpose: string;
  storageKey: string;
  contentType: string;
  status: string;
  downloadUrl?: string | null;
};

type WishImageGenerationJobResponse = {
  id: string;
  familyId: string;
  childId: string;
  wishId?: string | null;
  usageCode: string;
  providerCode?: string | null;
  title: string;
  note?: string | null;
  category?: string | null;
  style: string;
  aspectRatio: string;
  status: string;
  mediaAssetId?: string | null;
  media?: MediaAssetResponse | null;
  modelProvider?: string | null;
  modelName?: string | null;
  attemptCount: number;
  errorCode?: string | null;
  errorMessage?: string | null;
  createdAt: string;
  updatedAt: string;
};

type CreateWishImageGenerationInput = {
  familyId: string;
  childId: string;
  wishId?: string;
  title: string;
  note?: string;
  providerCode?: string;
  category?: string;
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
  if (!phoneNumber) redirect(`/?authError=${encodeURIComponent("请输入手机号")}`);
  try {
    const result = await corePostJson<PhoneCodeCreated>("/auth/phone-codes", { phoneNumber, purpose: "login" });
    redirect(
      `/?phone=${encodeURIComponent(phoneNumber)}&verificationToken=${encodeURIComponent(result.verificationToken)}&debugCode=${encodeURIComponent(
        result.debugCode ?? ""
      )}`
    );
  } catch (error) {
    if (isRedirectError(error)) throw error;
    redirect(`/?authError=${encodeURIComponent(errorMessage(error))}`);
  }
}

export async function loginParentAction(formData: FormData) {
  const verificationToken = valueFromForm(formData, "verificationToken");
  const code = valueFromForm(formData, "code");
  if (!verificationToken || !code) redirect(`/?authError=${encodeURIComponent("请输入验证码")}`);

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
    if (isRedirectError(error)) throw error;
    redirect(`/?authError=${encodeURIComponent(errorMessage(error))}`);
  }
}

export async function selectParentChildAction(formData: FormData) {
  const familyId = valueFromForm(formData, "familyId");
  const childId = valueFromForm(formData, "childId");
  if (!familyId || !childId) redirect(`/?setup=1&setupError=${encodeURIComponent("请选择家庭和孩子")}`);
  await updateParentWebSession({ familyId, childId });
  redirect("/");
}

export async function completeParentFamilySetupAction(formData: FormData) {
  const session = await requireParentSession();
  const familyName = valueFromForm(formData, "familyName");
  const timezone = valueFromForm(formData, "timezone") ?? "Asia/Shanghai";
  const childNickname = valueFromForm(formData, "childNickname");
  if (!familyName || !childNickname) redirect(`/?setup=1&setupError=${encodeURIComponent("请补全家庭名称和孩子昵称")}`);

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

export async function approveReviewAction(...args: unknown[]) {
  await submitReview(formDataFromActionArgs(args), "approved");
}

export async function requestRevisionAction(...args: unknown[]) {
  await submitReview(formDataFromActionArgs(args), "needs_revision");
}

export async function processReviewAction(formData: FormData): Promise<{ ok: true } | { ok: false; message: string }> {
  const submissionId = valueFromForm(formData, "submissionId");
  const decision = valueFromForm(formData, "decision");
  if (!submissionId || (decision !== "approved" && decision !== "needs_revision")) return { ok: false, message: "审核参数不完整。" };
  try {
    const config = await requireParentActionConfig();
    if (!config.accessToken) return { ok: false, message: "登录状态已失效，请重新登录。" };
    await coreRequestJson<Record<string, unknown>>("POST", "/reviews", {
      submissionId,
      decision,
      feedback: { emoji: decision === "approved" ? "heart" : "seed", text: valueFromForm(formData, "feedbackText") ?? "" }
    }, config.accessToken, `parent-web-${decision}-${submissionId}`);
    revalidatePath("/reviews");
    revalidatePath("/", "layout");
    return { ok: true };
  } catch (error) {
    return { ok: false, message: errorMessage(error) };
  }
}

export async function skipTaskAction(...args: unknown[]) {
  const formData = formDataFromActionArgs(args);
  const taskId = requireFormValue(formData, "taskId", "缺少任务信息，无法跳过。", "/");
  try {
    await postCore(`/tasks/${taskId}/skip`, {
      reason: valueFromForm(formData, "reason") ?? "家庭临时调整"
    });
    revalidatePath("/", "layout");
    actionSuccessRedirect(formData, "/", "已跳过任务。");
  } catch (error) {
    if (isRedirectError(error)) throw error;
    actionErrorRedirect(formData, "/", errorMessage(error));
  }
}

export async function postponeTaskAction(...args: unknown[]) {
  const formData = formDataFromActionArgs(args);
  const taskId = requireFormValue(formData, "taskId", "缺少任务信息，无法延后。", "/");
  const newDate = requireFormValue(formData, "newDate", "缺少延后日期，无法延后任务。", "/");
  try {
    await postCore(`/tasks/${taskId}/postpone`, {
      newDate,
      reason: valueFromForm(formData, "reason") ?? "顺延到下一天"
    });
    revalidatePath("/", "layout");
    actionSuccessRedirect(formData, "/", "已延后任务。");
  } catch (error) {
    if (isRedirectError(error)) throw error;
    actionErrorRedirect(formData, "/", errorMessage(error));
  }
}

export async function createTaskTemplateAction(...args: unknown[]) {
  const formData = formDataFromActionArgs(args);
  const title = valueFromForm(formData, "title");
  const category = valueFromForm(formData, "category");
  const submissionType = valueFromForm(formData, "submissionType");
  if (!title || !category || !submissionType) {
    actionErrorRedirect(formData, "/plan", "请补全任务模板标题、分类和提交类型。");
  }

  try {
    const config = await requireParentActionConfig();
    if (!config.familyId) throw new Error("请选择家庭后再创建任务模板。");

    const defaultDurationSec = valueFromForm(formData, "defaultDurationSec");
    await postCore("/task-templates", {
      familyId: config.familyId,
      title,
      category,
      submissionType,
      description: valueFromForm(formData, "description") ?? undefined,
      targetText: valueFromForm(formData, "targetText") ?? undefined,
      defaultDurationSec: defaultDurationSec ? Number(defaultDurationSec) : undefined
    });
    revalidatePath("/plan");
    actionSuccessRedirect(formData, "/plan", "任务模板已创建。");
  } catch (error) {
    if (isRedirectError(error)) throw error;
    actionErrorRedirect(formData, "/plan", errorMessage(error));
  }
}

export async function saveWeeklyPlanAction(...args: unknown[]) {
  const formData = formDataFromActionArgs(args);
  const rulesJson = valueFromForm(formData, "rulesJson");
  const childId = valueFromForm(formData, "childId");
  const weekId = valueFromForm(formData, "weekId");
  const startDate = valueFromForm(formData, "startDate");
  const endDate = valueFromForm(formData, "endDate");
  if (!rulesJson || !childId || !weekId || !startDate || !endDate) {
    actionErrorRedirect(formData, "/plan", "缺少周计划信息，无法保存。");
  }
  try {
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
    revalidatePath("/", "layout");
    actionSuccessRedirect(formData, "/plan", "周计划已保存。");
  } catch (error) {
    if (isRedirectError(error)) throw error;
    actionErrorRedirect(formData, "/plan", errorMessage(error));
  }
}

export async function createPairingSessionAction(...args: unknown[]) {
  const formData = formDataFromActionArgs(args);
  const childId = requireFormValue(formData, "childId", "缺少孩子信息，无法生成配对码。", "/#settings");
  try {
    const config = await requireParentActionConfig();
    if (!config.familyId) throw new Error("请选择家庭后再生成配对码。");
    const session = await postCore(`/families/${config.familyId}/pairing-sessions`, { childId }, { idempotent: false });
    const pairingCode = typeof session.pairingCode === "string" ? session.pairingCode : "";
    const expiresAt = typeof session.expiresAt === "string" ? session.expiresAt : "";
    redirect(
      `/?pairingCode=${encodeURIComponent(pairingCode)}&pairingExpiresAt=${encodeURIComponent(expiresAt)}&actionSuccess=${encodeURIComponent(
        "配对码已生成。"
      )}#settings`
    );
  } catch (error) {
    if (isRedirectError(error)) throw error;
    actionErrorRedirect(formData, "/#settings", errorMessage(error));
  }
}

export async function createWishAction(...args: unknown[]) {
  const formData = formDataFromActionArgs(args);
  const childId = valueFromForm(formData, "childId");
  const title = valueFromForm(formData, "title");
  const weekId = valueFromForm(formData, "weekId");
  if (!childId || !title || !weekId) {
    actionErrorRedirect(formData, "/wish", "请填写心愿标题，并确认孩子和周计划信息完整。");
  }
  try {
    const config = await requireParentActionConfig();
    if (!config.familyId) throw new Error("请选择家庭后再创建心愿。");
    const imageMediaId = valueFromForm(formData, "imageMediaId") ?? undefined;
    const wish = await postCore("/wishes", {
      familyId: config.familyId,
      childId,
      weekId,
      title,
      note: valueFromForm(formData, "note"),
      imageMediaId,
      requiredFragments: Number(valueFromForm(formData, "requiredFragments") ?? "10"),
      rewardMode: valueFromForm(formData, "rewardMode") ?? "flexible",
      fragmentVisualMode: valueFromForm(formData, "fragmentVisualMode") ?? "grid_reveal"
    });
    const wishId = typeof wish.id === "string" ? wish.id : "";
    if (!wishId) throw new Error("创建心愿响应缺少心愿ID，请重试。");
    if (imageMediaId) await attachWishImage(wishId, imageMediaId);
    await postCore(`/wishes/${wishId}/activate`, {});
    revalidatePath("/", "layout");
    actionSuccessRedirect(formData, "/wish", "心愿已创建并激活。");
  } catch (error) {
    if (isRedirectError(error)) throw error;
    actionErrorRedirect(formData, "/wish", errorMessage(error));
  }
}

type WishUploadFileInfo = {
  contentType: string;
  sizeBytes: number;
};

export async function createWishUploadSession(familyId: string, childId: string, fileInfo: WishUploadFileInfo) {
  const config = await requireParentActionConfig();
  if (!config.accessToken) throw new Error("请先登录家长账号。");
  if (!config.familyId || config.familyId !== familyId) throw new Error("家庭信息不匹配，请刷新后重试。");
  if (!childId || childId !== config.childId) throw new Error("孩子信息不匹配，请刷新后重试。");
  if (!fileInfo.contentType.startsWith("image/")) throw new Error("心愿图片只能上传图片文件。");
  if (!Number.isFinite(fileInfo.sizeBytes) || fileInfo.sizeBytes <= 0) throw new Error("图片文件无效，请重新选择。");

  const session = await corePostJson<{ mediaId: string; uploadUrl: string }>(
    "/media/upload-sessions",
    {
      familyId,
      childId,
      purpose: "wish_image",
      contentType: fileInfo.contentType,
      sizeBytes: fileInfo.sizeBytes
    },
    config.accessToken,
    `parent-web-wish-image-${Date.now()}`
  );
  if (!session.mediaId || !session.uploadUrl) throw new Error("上传会话响应不完整，请重试。");
  return { mediaId: session.mediaId, uploadUrl: session.uploadUrl };
}

export async function finalizeWishUpload(mediaId: string) {
  const config = await requireParentActionConfig();
  if (!config.accessToken) throw new Error("请先登录家长账号。");
  if (!mediaId) throw new Error("缺少图片信息，请重新上传。");
  await corePostJson<Record<string, unknown>>(
    `/media/${mediaId}/finalize`,
    {},
    config.accessToken,
    `parent-web-wish-image-finalize-${mediaId}`
  );
  return { mediaId };
}

type WishRedemptionUploadSession = {
  mediaId: string;
  uploadUrl: string;
  maxSizeBytes?: number;
};

export async function createWishRedemptionUploadSession(
  familyId: string,
  childId: string,
  fileInfo: WishUploadFileInfo
): Promise<WishRedemptionUploadSession> {
  const config = await requireParentActionConfig();
  if (!config.accessToken) throw new Error("请先登录家长账号。");
  if (!config.familyId || config.familyId !== familyId) throw new Error("家庭信息不匹配，请刷新后重试。");
  if (!childId || childId !== config.childId) throw new Error("孩子信息不匹配，请刷新后重试。");
  if (!fileInfo.contentType.startsWith("image/")) throw new Error("兑现照片只能上传图片文件。");
  if (!Number.isFinite(fileInfo.sizeBytes) || fileInfo.sizeBytes <= 0) throw new Error("图片文件无效，请重新选择。");

  const session = await corePostJson<WishRedemptionUploadSession>(
    "/media/upload-sessions",
    { familyId, childId, purpose: "wish_redemption", contentType: fileInfo.contentType, sizeBytes: fileInfo.sizeBytes },
    config.accessToken,
    `parent-web-wish-redemption-upload-${Date.now()}`
  );
  if (!session.mediaId || !session.uploadUrl) throw new Error("上传会话响应不完整，请重试。");
  return session;
}

export async function finalizeWishRedemptionUpload(mediaId: string) {
  const config = await requireParentActionConfig();
  if (!config.accessToken) throw new Error("请先登录家长账号。");
  if (!mediaId.trim()) throw new Error("缺少照片信息，请重新上传。");
  await corePostJson<Record<string, unknown>>(
    `/media/${encodeURIComponent(mediaId)}/finalize`,
    {},
    config.accessToken,
    `parent-web-wish-redemption-finalize-${mediaId}`
  );
  return { mediaId };
}

export async function redeemWish(input: {
  wishId: string;
  redeemedDate: string;
  photoMediaIds: string[];
  parentNote?: string;
  childNote?: string;
}) {
  const config = await requireParentActionConfig();
  if (!config.accessToken) throw new Error("请先登录家长账号。");
  if (!input.wishId.trim()) throw new Error("缺少心愿信息，请刷新后重试。");
  if (!/^\d{4}-\d{2}-\d{2}$/.test(input.redeemedDate)) throw new Error("请选择有效的兑现日期。");
  if (input.photoMediaIds.length < 1 || input.photoMediaIds.length > 3) throw new Error("请上传 1-3 张兑现照片。");

  const result = await corePostJson<Record<string, unknown>>(
    `/wishes/${encodeURIComponent(input.wishId.trim())}/redeem`,
    {
      redeemedDate: input.redeemedDate,
      photoMediaIds: input.photoMediaIds,
      parentNote: input.parentNote?.trim() || undefined,
      childNote: input.childNote?.trim() || undefined
    },
    config.accessToken,
    `parent-web-wish-redeem-${input.wishId.trim()}-${input.redeemedDate}`
  );
  revalidatePath("/wish");
  revalidatePath("/", "layout");
  return result;
}

export async function createWishImageGeneration(input: CreateWishImageGenerationInput): Promise<WishImageGenerationJobResponse> {
  const config = await requireParentActionConfig();
  if (!config.accessToken) throw new Error("请先登录家长账号。");
  if (!config.familyId || config.familyId !== input.familyId) throw new Error("家庭信息不匹配，请刷新后重试。");
  if (!input.childId || input.childId !== config.childId) throw new Error("孩子信息不匹配，请刷新后重试。");
  const title = input.title.trim();
  if (!title) throw new Error("先填写心愿标题。");

  return corePostJson<WishImageGenerationJobResponse>(
    "/wishes/image-generations",
    {
      familyId: input.familyId,
      childId: input.childId,
      wishId: input.wishId?.trim() || undefined,
      usageCode: "wish_card",
      providerCode: input.providerCode?.trim() || undefined,
      title,
      note: input.note?.trim() || undefined,
      category: input.category?.trim() || undefined,
      style: "warm_illustration",
      aspectRatio: "1:1"
    },
    config.accessToken,
    `parent-web-wish-image-generation-${Date.now()}`
  );
}

export async function getWishImageGenerationJob(jobId: string): Promise<WishImageGenerationJobResponse> {
  const config = await requireParentActionConfig();
  if (!config.accessToken) throw new Error("请先登录家长账号。");
  if (!jobId.trim()) throw new Error("缺少图片生成任务信息，请重试。");
  return coreGetJson<WishImageGenerationJobResponse>(`/wishes/image-generations/${encodeURIComponent(jobId.trim())}`, config.accessToken);
}

export async function attachWishImage(wishId: string, mediaId: string, sourceType = "reused") {
  const config = await requireParentActionConfig();
  if (!config.accessToken) throw new Error("请先登录家长账号。");
  if (!wishId.trim() || !mediaId.trim()) throw new Error("缺少心愿或图片信息，请重试。");
  return corePostJson<Record<string, unknown>>(
    `/wishes/${encodeURIComponent(wishId.trim())}/image`,
    { mediaId: mediaId.trim(), sourceType },
    config.accessToken,
    `parent-web-wish-image-attach-${wishId.trim()}-${mediaId.trim()}`
  );
}

export async function exportMemoryAction(...args: unknown[]) {
  const formData = formDataFromActionArgs(args);
  const memoryId = requireFormValue(formData, "memoryId", "缺少纪念册信息，无法导出。", "/memories");
  try {
    await postCore(`/memories/${memoryId}/export`, {
      format: valueFromForm(formData, "format") ?? "pdf"
    });
    revalidatePath("/", "layout");
    actionSuccessRedirect(formData, "/memories", "纪念册导出请求已提交。");
  } catch (error) {
    if (isRedirectError(error)) throw error;
    actionErrorRedirect(formData, "/memories", errorMessage(error));
  }
}

export async function arrangeRoomItemAction(...args: unknown[]) {
  const formData = formDataFromActionArgs(args);
  const itemId = requireFormValue(formData, "itemId", "缺少小屋物件信息，无法调整摆放。", "/room");
  try {
    await postCore(`/room/items/${itemId}/arrange`, {
      position: {
        x: Number(valueFromForm(formData, "x") ?? "16"),
        y: Number(valueFromForm(formData, "y") ?? "32")
      }
    });
    revalidatePath("/", "layout");
    actionSuccessRedirect(formData, "/room", "小屋摆放已更新。");
  } catch (error) {
    if (isRedirectError(error)) throw error;
    actionErrorRedirect(formData, "/room", errorMessage(error));
  }
}

export async function markNotificationsReadAction(...args: unknown[]) {
  const formData = formDataFromActionArgs(args);
  const ids = valuesFromForm(formData, "notificationIds");
  if (ids.length === 0) return;
  try {
    await postCore(
      "/notifications/read",
      {
        notificationIds: ids
      },
      { idempotent: false }
    );
    revalidatePath("/", "layout");
    actionSuccessRedirect(formData, "/notifications", "通知已标记为已读。");
  } catch (error) {
    if (isRedirectError(error)) throw error;
    actionErrorRedirect(formData, "/notifications", errorMessage(error));
  }
}

export async function updateNotificationPreferenceAction(...args: unknown[]) {
  const formData = formDataFromActionArgs(args);
  const notificationType = requireFormValue(formData, "notificationType", "缺少通知类型，无法更新偏好。", "/notifications");
  try {
    const config = await requireParentActionConfig();
    if (!config.familyId) throw new Error("请选择家庭后再更新通知偏好。");
    const clearQuietHours = valueFromForm(formData, "clearQuietHours") === "true" && valueFromForm(formData, "savePreference") !== "true";
    const quietHoursStart = valueFromForm(formData, "quietHoursStart");
    const quietHoursEnd = valueFromForm(formData, "quietHoursEnd");
    const quietHoursTimezone = valueFromForm(formData, "quietHoursTimezone") ?? "Asia/Shanghai";
    await putCore("/notification-preferences", {
      familyId: config.familyId,
      notificationType,
      enabled: valueFromForm(formData, "enabled") === "true",
      channels: {
        inbox: valueFromForm(formData, "inbox") === "true",
        push: valueFromForm(formData, "push") === "true"
      },
      quietHours: clearQuietHours || !quietHoursStart || !quietHoursEnd
        ? {}
        : { start: quietHoursStart, end: quietHoursEnd, timezone: quietHoursTimezone }
    });
    revalidatePath("/", "layout");
    actionSuccessRedirect(formData, "/notifications", "通知偏好已更新。");
  } catch (error) {
    if (isRedirectError(error)) throw error;
    actionErrorRedirect(formData, "/notifications", `通知偏好保存失败：${notificationPreferenceErrorMessage(error)}`);
  }
}

function notificationPreferenceErrorMessage(error: unknown) {
  const message = errorMessage(error);
  if (message.includes("Unsupported") || message.includes("invalid") || message.includes("cannot")) {
    return "服务端拒绝了这组通知设置，请检查后重试。";
  }
  if (message.includes("Unauthorized") || message.includes("登录")) return "登录状态已失效，请重新登录。";
  return "网络或服务暂时不可用，请稍后重试。";
}

async function submitReview(formData: FormData, decision: ReviewDecision) {
  const submissionId = requireFormValue(formData, "submissionId", "缺少提交记录，无法完成审核。", "/reviews");

  try {
    const config = await requireParentActionConfig();
    if (!config.accessToken) {
      throw new Error("请先登录家长账号。");
    }

    const feedbackText =
      valueFromForm(formData, "feedbackText") ??
      (decision === "approved" ? "看到了，完成得很好。" : "请再补充一点，让记录更完整。");
    await coreRequestJson<Record<string, unknown>>("POST", "/reviews", {
        submissionId,
        decision,
        feedback: {
          emoji: decision === "approved" ? "heart" : "seed",
          text: feedbackText
        }
      }, config.accessToken, `parent-web-${decision}-${submissionId}`);

    revalidatePath("/", "layout");
    actionSuccessRedirect(formData, "/reviews", decision === "approved" ? "已通过提交。" : "已退回提交。");
  } catch (error) {
    if (isRedirectError(error)) throw error;
    actionErrorRedirect(formData, "/reviews", errorMessage(error));
  }
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
  return coreRequestJson<Record<string, unknown>>(method, path, body, config.accessToken, options.idempotent === false ? undefined : `parent-web-${path.replace(/[^a-zA-Z0-9]/g, "-")}-${Date.now()}`);
}

function formDataFromActionArgs(args: unknown[]): FormData {
  const formData = args.find(isFormData);
  if (formData) return formData;

  const record = args.find(isRecord);
  const normalized = new FormData();
  if (!record) return normalized;
  for (const [key, value] of Object.entries(record)) {
    appendFormDataValue(normalized, key, value);
  }
  return normalized;
}

function appendFormDataValue(formData: FormData, key: string, value: unknown) {
  if (value == null) return;
  if (Array.isArray(value)) {
    value.forEach((item) => appendFormDataValue(formData, key, item));
    return;
  }
  if (typeof value === "string" || typeof value === "number" || typeof value === "boolean") {
    formData.append(key, String(value));
  }
}

function isFormData(value: unknown): value is FormData {
  return (
    typeof value === "object" &&
    value !== null &&
    typeof (value as FormData).get === "function" &&
    typeof (value as FormData).getAll === "function" &&
    typeof (value as FormData).entries === "function"
  );
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !isFormData(value);
}

function valueFromForm(formData: FormData, key: string): string | null {
  return valuesFromForm(formData, key)[0] ?? null;
}

function valuesFromForm(formData: FormData, key: string): string[] {
  const values: string[] = [];
  for (const [entryKey, value] of formData.entries()) {
    if (entryKey !== key && !entryKey.endsWith(`_${key}`)) continue;
    if (typeof value !== "string") continue;
    const trimmed = value.trim();
    if (trimmed.length > 0) values.push(trimmed);
  }
  return values;
}

function requireFormValue(formData: FormData, key: string, message: string, fallbackPath: string): string {
  const value = valueFromForm(formData, key);
  if (!value) actionErrorRedirect(formData, fallbackPath, message);
  return value;
}

function actionErrorRedirect(formData: FormData, fallbackPath: string, message: string): never {
  actionMessageRedirect(formData, fallbackPath, "actionError", message);
}

function actionSuccessRedirect(formData: FormData, fallbackPath: string, message: string): never {
  actionMessageRedirect(formData, fallbackPath, "actionSuccess", message);
}

function actionMessageRedirect(formData: FormData, fallbackPath: string, queryKey: "actionError" | "actionSuccess", message: string): never {
  const returnTo = valueFromForm(formData, "returnTo") ?? fallbackPath;
  const hashIndex = returnTo.indexOf("#");
  const base = hashIndex >= 0 ? returnTo.slice(0, hashIndex) : returnTo;
  const hash = hashIndex >= 0 ? returnTo.slice(hashIndex) : "";
  const queryParts = [`${queryKey}=${encodeURIComponent(message)}`];
  if (queryKey === "actionError" && valueFromForm(formData, "preserveFeedback") === "true") {
    const feedbackText = valueFromForm(formData, "feedbackText");
    if (feedbackText) queryParts.push(`feedbackText=${encodeURIComponent(feedbackText)}`);
  }
  const separator = base.includes("?") ? "&" : "?";
  redirect(`${base}${separator}${queryParts.join("&")}${hash}`);
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
