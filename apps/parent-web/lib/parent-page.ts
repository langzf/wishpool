import { redirect } from "next/navigation";
import { isCoreApiUnauthorizedError } from "@/lib/core-client";
import { isParentWebUnauthorizedError, loadParentDashboardData, type ParentDashboardData } from "@/lib/dashboard-data";
import { loadParentProfile, type ParentProfileData } from "@/lib/profile-data";
import { getParentWebSession, type ParentWebSession } from "@/lib/session";

export type ParentPageContext = {
  session: ParentWebSession | null;
  profile: ParentProfileData | null;
  profileError?: string;
  data: ParentDashboardData | null;
  dataError?: string;
  needsSetup: boolean;
};

export async function requireParentPageContext(): Promise<ParentPageContext> {
  const session = await getParentWebSession();
  if (!session) {
    return {
      session: null,
      profile: null,
      data: null,
      needsSetup: false
    };
  }

  let profile: ParentProfileData | null = null;
  let profileError: string | undefined;
  try {
    profile = await loadParentProfile(session);
  } catch (error) {
    if (isCoreApiUnauthorizedError(error)) redirect("/auth-expired");
    profileError = error instanceof Error ? error.message : "服务暂时不可用，请稍后重试。";
  }
  if (!profile) {
    return {
      session,
      profile: null,
      profileError,
      data: null,
      needsSetup: false
    };
  }

  if (!profile.selectedFamilyId || !profile.selectedChildId) {
    return {
      session,
      profile,
      data: null,
      needsSetup: true
    };
  }

  const resolvedSession = { ...session, familyId: profile.selectedFamilyId, childId: profile.selectedChildId };
  const result = await loadParentDashboardData(resolvedSession).then(
    (data) => ({ data, dataError: undefined }),
    (error) => {
      if (isParentWebUnauthorizedError(error)) redirect("/auth-expired");
      return { data: null, dataError: error instanceof Error ? error.message : "服务暂时不可用，请稍后重试。" };
    }
  );

  return {
    session: resolvedSession,
    profile,
    profileError,
    data: result.data,
    dataError: result.dataError,
    needsSetup: false
  };
}
