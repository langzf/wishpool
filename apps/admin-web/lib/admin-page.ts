import { redirect } from "next/navigation";
import {
  isAdminWebUnauthorizedError,
  loadAdminDashboardData,
  type AdminDashboardData
} from "@/lib/dashboard-data";
import { getAdminWebSession, type AdminWebSession } from "@/lib/session";

export type AdminPageContext =
  | {
      session: null;
      data: null;
    }
  | {
      session: AdminWebSession;
      data: AdminDashboardData;
    };

export async function requireAdminPageContext(): Promise<AdminPageContext> {
  const session = await getAdminWebSession();
  if (!session) {
    return {
      session: null,
      data: null
    };
  }

  const data = await loadAdminDashboardData(session).catch((error) => {
    if (isAdminWebUnauthorizedError(error)) redirect("/auth-expired");
    throw error;
  });

  return {
    session,
    data
  };
}
