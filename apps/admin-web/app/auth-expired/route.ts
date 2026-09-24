import { redirect } from "next/navigation";
import { clearAdminWebSession } from "@/lib/session";

export async function GET() {
  await clearAdminWebSession();
  redirect("/?authError=登录状态已过期，请重新登录。");
}
