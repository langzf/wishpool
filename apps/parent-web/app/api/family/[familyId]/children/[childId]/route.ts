import { NextResponse } from "next/server";
import { coreGetJson, coreRequestJson } from "@/lib/core-client";
import { getParentWebSession } from "@/lib/session";
type Params = { params: Promise<{ familyId: string; childId: string }> };
type MeResponse = { families: Array<{ family: { id: string }; member: { role: string } }> };
async function ownerSession(familyId: string) {
  const session = await getParentWebSession();
  if (!session?.accessToken) return { error: "请先登录家长账号。" };
  const me = await coreGetJson<MeResponse>("/me", session.accessToken);
  const family = me.families.find((item) => item.family.id === familyId);
  if (!family) return { error: "你无权访问该家庭。" };
  if (family.member.role !== "parent_owner") return { error: "只有家长所有者可以修改儿童资料。" };
  return { session };
}
export async function PATCH(request: Request, context: Params) {
  try { const { familyId, childId } = await context.params; const result = await ownerSession(familyId); if (result.error || !result.session) return NextResponse.json({ error: result.error ?? "请先登录家长账号。" }, { status: 403 }); const child = await coreRequestJson("PATCH", `/children/${childId}`, await request.json(), result.session.accessToken); return NextResponse.json(child); }
  catch (error) { return NextResponse.json({ error: error instanceof Error ? error.message : "儿童资料保存失败，请稍后重试。" }, { status: 400 }); }
}
