import { NextResponse } from "next/server";
import { coreGetJson } from "@/lib/core-client";
import { getParentWebSession } from "@/lib/session";
export async function GET(_request: Request, context: { params: Promise<{ familyId: string }> }) { const { familyId } = await context.params; const session = await getParentWebSession(); if (!session?.accessToken) return NextResponse.json({ error: "请先登录家长账号。" }, { status: 401 }); try { return NextResponse.json(await coreGetJson(`/families/${familyId}/devices`, session.accessToken)); } catch { return NextResponse.json({ error: "设备列表加载失败，请稍后重试。" }, { status: 502 }); } }
