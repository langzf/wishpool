import { ParentAuthPanel, ParentSetupPanel } from "@/components/AuthPanel";
import { ParentDashboard } from "@/components/Dashboard";
import { Shell } from "@/components/Shell";
import { loadParentDashboardData } from "@/lib/dashboard-data";
import { loadParentProfile } from "@/lib/profile-data";
import { getParentWebSession } from "@/lib/session";

type PageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function Page({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const session = await getParentWebSession();
  if (!session) {
    return (
      <ParentAuthPanel
        debugCode={singleParam(params.debugCode)}
        error={singleParam(params.authError)}
        phone={singleParam(params.phone)}
        verificationToken={singleParam(params.verificationToken)}
      />
    );
  }

  const profile = await loadParentProfile(session);
  if (!profile.selectedFamilyId || !profile.selectedChildId || singleParam(params.setup) === "1") {
    return (
      <Shell>
        <ParentSetupPanel error={singleParam(params.setupError)} profile={profile} />
      </Shell>
    );
  }

  const resolvedSession = { ...session, familyId: profile.selectedFamilyId, childId: profile.selectedChildId };
  const data = await loadParentDashboardData(resolvedSession);
  const pairingCode = singleParam(params.pairingCode);
  const pairingExpiresAt = singleParam(params.pairingExpiresAt);

  return (
    <Shell>
      <ParentDashboard data={data} pairingCode={pairingCode} pairingExpiresAt={pairingExpiresAt} />
    </Shell>
  );
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
