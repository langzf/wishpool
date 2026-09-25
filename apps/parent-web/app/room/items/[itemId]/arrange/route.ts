import { NextResponse } from "next/server";
import { coreRequestJson } from "@/lib/core-client";
import { getParentWebSession } from "@/lib/session";

export async function POST(request: Request, context: { params: Promise<{ itemId: string }> }) {
  const session = await getParentWebSession();
  if (!session?.accessToken) return NextResponse.json({ message: "请先登录家长账号。" }, { status: 401 });
  try {
    const { itemId } = await context.params;
    const result = await coreRequestJson("POST", `/room/items/${itemId}/arrange`, await request.json(), session.accessToken, request.headers.get("Idempotency-Key") ?? undefined);
    return NextResponse.json(result);
  } catch (error) {
    return NextResponse.json({ message: error instanceof Error ? error.message : "小屋摆放失败。" }, { status: 400 });
  }
}
