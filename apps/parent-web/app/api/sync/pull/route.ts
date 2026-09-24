import { NextResponse } from "next/server";
import { buildAuthHeaders, getApiRuntimeConfig } from "@/lib/api";
import { getParentWebSession } from "@/lib/session";

export async function GET(request: Request) {
  const session = await getParentWebSession();
  if (!session?.accessToken || !session.familyId) return NextResponse.json({ message: "请先登录家长账号。" }, { status: 401 });
  const requestUrl = new URL(request.url);
  const requestedFamilyId = requestUrl.searchParams.get("familyId");
  if (requestedFamilyId && requestedFamilyId !== session.familyId) return NextResponse.json({ message: "家庭信息不匹配。" }, { status: 403 });
  const afterSeq = parseInteger(requestUrl.searchParams.get("afterSeq"), 0, 0);
  const limit = parseInteger(requestUrl.searchParams.get("limit"), 500, 1, 1000);
  const upstreamUrl = new URL("/sync/pull", getApiRuntimeConfig().coreApiBaseUrl);
  upstreamUrl.searchParams.set("familyId", session.familyId);
  upstreamUrl.searchParams.set("afterSeq", String(afterSeq));
  upstreamUrl.searchParams.set("limit", String(limit));
  const upstream = await fetch(upstreamUrl, { cache: "no-store", headers: buildAuthHeaders(session.accessToken) });
  const body = await upstream.text();
  return new Response(body, {
    status: upstream.status,
    headers: { "Cache-Control": "no-store", "Content-Type": upstream.headers.get("content-type") ?? "application/json" }
  });
}

function parseInteger(value: string | null, fallback: number, minimum: number, maximum?: number) {
  const parsed = Number(value ?? fallback);
  if (!Number.isInteger(parsed) || parsed < minimum || (maximum !== undefined && parsed > maximum)) return fallback;
  return parsed;
}
