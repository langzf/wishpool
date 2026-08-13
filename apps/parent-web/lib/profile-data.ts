import { coreGetJson } from "@/lib/core-client";
import type { ParentWebSession } from "@/lib/session";

export type FamilyContext = {
  family: {
    id: string;
    name: string;
    timezone: string;
    status: string;
  };
  member: {
    id: string;
    familyId: string;
    userId: string;
    role: string;
    childId?: string | null;
    displayName: string;
    status: string;
  };
};

export type ChildProfile = {
  id: string;
  familyId: string;
  nickname: string;
  birthYear?: number | null;
  avatarAsset?: string | null;
  roomTheme: string;
  status: string;
};

export type ParentProfileData = {
  userName: string;
  families: Array<FamilyContext & { children: ChildProfile[] }>;
  selectedFamilyId?: string;
  selectedChildId?: string;
};

type MeResponse = {
  user: { displayName: string };
  families: FamilyContext[];
};

export async function loadParentProfile(session: ParentWebSession): Promise<ParentProfileData> {
  const me = await coreGetJson<MeResponse>("/me", session.accessToken);
  const families = await Promise.all(
    me.families.map(async (context) => ({
      ...context,
      children: await coreGetJson<ChildProfile[]>(`/families/${context.family.id}/children`, session.accessToken)
    }))
  );

  const selectedFamily = families.find((context) => context.family.id === session.familyId) ?? families[0];
  const selectedChild =
    selectedFamily?.children.find((child) => child.id === session.childId) ??
    selectedFamily?.children.find((child) => child.status === "active") ??
    selectedFamily?.children[0];

  return {
    userName: session.userName ?? me.user.displayName,
    families,
    selectedFamilyId: selectedFamily?.family.id,
    selectedChildId: selectedChild?.id
  };
}
