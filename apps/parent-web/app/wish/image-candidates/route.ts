import { NextResponse } from "next/server";
import { buildAuthHeaders, getApiRuntimeConfig } from "@/lib/api";
import { getParentWebSession } from "@/lib/session";

export async function POST(request: Request) {
  const session = await getParentWebSession();
  const config = getApiRuntimeConfig();
  const accessToken = session?.accessToken ?? config.accessToken;
  if (!accessToken) {
    return NextResponse.json({ items: [] }, { status: 401 });
  }

  const body = (await request.json()) as Record<string, unknown>;
  const response = await fetch(`${config.coreApiBaseUrl}/wishes/image-candidates`, {
    method: "POST",
    cache: "no-store",
    headers: {
      ...buildAuthHeaders(accessToken),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(body)
  });

  const text = await response.text();
  return new NextResponse(text, {
    status: response.status,
    headers: {
      "Content-Type": response.headers.get("Content-Type") ?? "application/json"
    }
  });
}
