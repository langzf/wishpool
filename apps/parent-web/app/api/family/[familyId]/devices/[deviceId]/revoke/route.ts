import { NextResponse } from "next/server";
import { corePostJson } from "@/lib/core-client";
import { getParentWebSession } from "@/lib/session";
export async function POST(request: Request, context: { params: Promise<{ familyId: string; deviceId: string }> }) { const { familyId, deviceId } = await context.params; const session = await getParentWebSession(); if (!session?.accessToken) return NextResponse.json({ error: "请先登录家长账号。" }, { status: 401 }); try { return NextResponse.json(await corePostJson(`/families/${familyId}/devices/${deviceId}/revoke`, await request.json(), session.accessToken)); } catch { return NextResponse.json({ error: "设备撤销失败，请稍后重试。" }, { status: 502 }); } }
