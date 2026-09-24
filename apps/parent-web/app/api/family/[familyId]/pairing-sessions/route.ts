import { NextResponse } from "next/server";
import { coreGetJson, corePostJson } from "@/lib/core-client";
import { getParentWebSession } from "@/lib/session";
type Params = { params: Promise<{ familyId: string }> };
type MeResponse = { families: Array<{ family: { id: string }; member: { role: string } }> };
export async function POST(request: Request, context: Params) {
  try { const { familyId } = await context.params; const session = await getParentWebSession(); if (!session?.accessToken) return NextResponse.json({ error: "请先登录家长账号。" }, { status: 401 }); const me = await coreGetJson<MeResponse>("/me", session.accessToken); const family = me.families.find((item) => item.family.id === familyId); if (!family || family.member.role !== "parent_owner") return NextResponse.json({ error: "只有家长所有者可以生成配对码。" }, { status: 403 }); const result = await corePostJson(`/families/${familyId}/pairing-sessions`, await request.json(), session.accessToken); return NextResponse.json(result, { status: 201 }); }
  catch (error) { return NextResponse.json({ error: error instanceof Error ? error.message : "配对码生成失败，请稍后重试。" }, { status: 400 }); }
}
