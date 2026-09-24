import { NextResponse } from "next/server";
import { corePostJson } from "@/lib/core-client";
import { loadParentProfile } from "@/lib/profile-data";
import { getParentWebSession } from "@/lib/session";

const confirmationTexts = { export: "EXPORT FAMILY DATA", delete: "DELETE FAMILY DATA" } as const;
type PrivacyType = keyof typeof confirmationTexts;
type PrivacyResponse = { id: string; familyId: string; requestType: PrivacyType; status: string; exportMedia?: unknown; createdAt: string };

export async function POST(request: Request) {
  try {
    const body = (await request.json()) as { familyId?: string; requestType?: PrivacyType; confirmationText?: string; reason?: string };
    const familyId = body.familyId?.trim();
    const requestType = body.requestType;
    const confirmationText = body.confirmationText?.trim();
    if (!familyId || !requestType || !(requestType in confirmationTexts)) return NextResponse.json({ error: "请求信息不完整。" }, { status: 400 });
    if (confirmationText !== confirmationTexts[requestType]) return NextResponse.json({ error: "确认词不正确，请按提示完整输入。" }, { status: 400 });
    const session = await getParentWebSession();
    if (!session) return NextResponse.json({ error: "请先登录家长账号。" }, { status: 401 });
    const profile = await loadParentProfile(session);
    const family = profile.families.find((item) => item.family.id === familyId);
    if (!family || family.member.role !== "parent_owner") return NextResponse.json({ error: "只有家庭所有者可以执行此操作。" }, { status: 403 });
    const result = await corePostJson<PrivacyResponse>(
      `/privacy/${requestType}`,
      { familyId, requestType, confirmationText, reason: body.reason?.trim() || undefined },
      session.accessToken,
      `parent-web-privacy-${requestType}-${familyId}-${Date.now()}`
    );
    return NextResponse.json(result, { status: 202 });
  } catch (error) {
    const message = error instanceof Error ? error.message : "隐私请求提交失败，请稍后重试。";
    return NextResponse.json({ error: toChineseError(message) }, { status: 500 });
  }
}

function toChineseError(message: string) {
  if (message.includes("401")) return "登录状态已失效，请重新登录。";
  if (message.includes("403")) return "只有家庭所有者可以执行此操作。";
  if (message.includes("400")) return "请求未通过校验，请检查确认词后重试。";
  return "隐私请求提交失败，请稍后重试。";
}
