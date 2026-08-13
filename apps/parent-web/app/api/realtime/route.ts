import { NextResponse } from "next/server";
import { getApiRuntimeConfig } from "@/lib/api";
import { getParentWebSession } from "@/lib/session";

export async function GET(request: Request) {
  const session = await getParentWebSession();
  if (!session?.accessToken || !session.familyId) {
    return NextResponse.json({ message: "请先登录家长账号。" }, { status: 401 });
  }

  const requestUrl = new URL(request.url);
  const afterSeq = requestUrl.searchParams.get("afterSeq") ?? "0";
  const config = getApiRuntimeConfig();
  const gatewayUrl = new URL("/realtime/sse", config.realtimeBaseUrl);
  gatewayUrl.searchParams.set("familyId", session.familyId);
  gatewayUrl.searchParams.set("afterSeq", afterSeq);

  const upstream = await fetch(gatewayUrl, {
    cache: "no-store",
    headers: {
      Authorization: `Bearer ${session.accessToken}`
    }
  });

  if (!upstream.ok || !upstream.body) {
    const errorBody = await upstream.text();
    return NextResponse.json({ message: errorBody || "实时同步连接失败。" }, { status: upstream.status || 502 });
  }

  return new Response(upstream.body, {
    status: 200,
    headers: {
      "Cache-Control": "no-cache, no-transform",
      "Content-Type": "text/event-stream",
      Connection: "keep-alive"
    }
  });
}
