import { Users } from "lucide-react";
import { Shell } from "@/components/Shell";
import { requireParentPageContext } from "@/lib/parent-page";
import { renderParentPageFallback } from "@/lib/parent-page-render";
import { coreGetJson } from "@/lib/core-client";
import { getParentWebSession } from "@/lib/session";
import { FamilySettingsClient, type FamilySettingsData } from "@/app/settings/family/FamilySettingsClient";
import type { ChildProfile, FamilyContext } from "@/lib/profile-data";

type FamilyMember = {
  id: string; familyId: string; userId: string; role: string; childId?: string | null; displayName: string; status: string;
};

export default async function FamilySettingsPage() {
  const ctx = await requireParentPageContext();
  const fallback = renderParentPageFallback(ctx);
  if (fallback) return fallback;
  if (!ctx.profile || !ctx.session) return null;
  const familyContext = ctx.profile.families.find((item) => item.family.id === ctx.session?.familyId) ?? ctx.profile.families[0];
  if (!familyContext) return <Shell><p className="form-error">当前没有可用的家庭信息。</p></Shell>;
  const session = await getParentWebSession();
  let members: FamilyMember[] = [];
  let membersError = "";
  if (session?.accessToken) {
    try { members = await coreGetJson<FamilyMember[]>(`/families/${familyContext.family.id}/members`, session.accessToken); }
    catch (error) { membersError = error instanceof Error ? error.message : "家庭成员加载失败，请稍后重试。"; }
  }
  const data: FamilySettingsData = { family: familyContext.family, member: familyContext.member, members, children: familyContext.children as ChildProfile[], membersError };
  return <Shell><header className="topbar"><div><p className="muted">管理家庭成员、儿童资料和设备连接</p><h1 className="page-title">家庭设置</h1></div><Users size={34} aria-hidden="true" /></header><FamilySettingsClient data={data} /></Shell>;
}
