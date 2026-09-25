import { NextResponse } from "next/server";
import { coreRequestJson } from "@/lib/core-client";
import { getParentWebSession } from "@/lib/session";

export async function POST(_request: Request, context: { params: Promise<{ itemId: string; action: string }> }) {
  const session = await getParentWebSession();
  if (!session?.accessToken) return NextResponse.json({ message: "请先登录家长账号。" }, { status: 401 });
  const { itemId, action } = await context.params;
  if (action !== "hide" && action !== "unhide") return NextResponse.json({ message: "不支持的操作。" }, { status: 404 });
  try {
    return NextResponse.json(await coreRequestJson("POST", `/room/items/${itemId}/${action}`, undefined, session.accessToken));
  } catch (error) {
    return NextResponse.json({ message: error instanceof Error ? error.message : "小屋可见性更新失败。" }, { status: 400 });
  }
}
