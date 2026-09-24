import { ParentAuthPanel, ParentSetupPanel } from "@/components/AuthPanel";
import { ParentDashboard } from "@/components/Dashboard";
import { Shell } from "@/components/Shell";
import { requireParentPageContext } from "@/lib/parent-page";
import { renderParentPageFallback } from "@/lib/parent-page-render";

type PageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function Page({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const { session, profile, profileError, data, dataError, needsSetup } = await requireParentPageContext();
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

  if (profileError || dataError || !profile) {
    return renderParentPageFallback({ session, profile, profileError, data, dataError, needsSetup });
  }
  if (needsSetup || singleParam(params.setup) === "1") {
    return (
      <Shell>
        <ParentSetupPanel error={singleParam(params.setupError)} profile={profile} />
      </Shell>
    );
  }
  if (!data) {
    return <ParentAuthPanel error="登录状态失效或核心服务暂不可用，请重新登录。" />;
  }

  const pairingCode = singleParam(params.pairingCode);
  const pairingExpiresAt = singleParam(params.pairingExpiresAt);

  return (
    <Shell>
      <ParentDashboard
        actionError={singleParam(params.actionError)}
        actionSuccess={singleParam(params.actionSuccess)}
        data={data}
        pairingCode={pairingCode}
        pairingExpiresAt={pairingExpiresAt}
      />
    </Shell>
  );
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
