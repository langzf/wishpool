import { redirect } from "next/navigation";
import { refreshParentSession } from "@/lib/core-client";
import { clearParentWebSession, getParentWebSession } from "@/lib/session";

export async function GET(request: Request) {
  const refreshFailed = new URL(request.url).searchParams.get("refreshFailed") === "1";
  if (refreshFailed) {
    await clearParentWebSession();
    redirect(`/?authError=${encodeURIComponent("登录状态已过期，请重新登录。")}`);
  }

  const session = await getParentWebSession();
  if (session?.refreshToken) {
    try {
      await refreshParentSession(session.refreshToken);
    } catch {
      // Continue to the signed-out path.
      await clearParentWebSession();
      redirect(`/?authError=${encodeURIComponent("登录状态已过期，请重新登录。")}`);
    }
    redirect("/");
  }
  await clearParentWebSession();
  redirect(`/?authError=${encodeURIComponent("登录状态已过期，请重新登录。")}`);
}
