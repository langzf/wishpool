import { coreGetJson } from "@/lib/core-client";
import type { ParentWebSession } from "@/lib/session";
import type { ChildProfile as ApiChildProfile } from "../../../packages/api-contracts/contract-ts/src/models/ChildProfile";
import type { FamilyMemberContext as ApiFamilyMemberContext } from "../../../packages/api-contracts/contract-ts/src/models/FamilyMemberContext";
import type { MeResponse as ApiMeResponse } from "../../../packages/api-contracts/contract-ts/src/models/MeResponse";

export type FamilyContext = ApiFamilyMemberContext;
export type ChildProfile = ApiChildProfile;

export type ParentProfileData = {
  userName: string;
  families: Array<FamilyContext & { children: ChildProfile[] }>;
  selectedFamilyId?: string;
  selectedChildId?: string;
};

type MeResponse = ApiMeResponse;

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
