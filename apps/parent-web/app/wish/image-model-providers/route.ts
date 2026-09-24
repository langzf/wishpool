import { NextResponse } from "next/server";
import { buildAuthHeaders, getApiRuntimeConfig } from "@/lib/api";
import { getParentWebSession } from "@/lib/session";

export async function GET() {
  const session = await getParentWebSession();
  const config = getApiRuntimeConfig();
  const accessToken = session?.accessToken ?? config.accessToken;
  if (!accessToken) {
    return NextResponse.json({ usageCode: "wish_card", selectedProviderCode: null, providers: [] }, { status: 401 });
  }

  const response = await fetch(`${config.coreApiBaseUrl}/wishes/image-model-providers?usageCode=wish_card`, {
    cache: "no-store",
    headers: buildAuthHeaders(accessToken)
  });

  const text = await response.text();
  return new NextResponse(text, {
    status: response.status,
    headers: {
      "Content-Type": response.headers.get("Content-Type") ?? "application/json"
    }
  });
}
