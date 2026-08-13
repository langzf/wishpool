import { AdminAuthPanel } from "@/components/AuthPanel";
import { AdminDashboard } from "@/components/AdminDashboard";
import { AdminShell } from "@/components/AdminShell";
import { loadAdminDashboardData } from "@/lib/dashboard-data";
import { getAdminWebSession } from "@/lib/session";

type PageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function Page({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const session = await getAdminWebSession();
  if (!session) {
    return <AdminAuthPanel error={singleParam(params.authError)} />;
  }

  const data = await loadAdminDashboardData(session);

  return (
    <AdminShell>
      <AdminDashboard
        data={data}
        mediaAccessAuditLogId={singleParam(params.mediaAccessAuditLogId)}
        mediaAccessExpiresAt={singleParam(params.mediaAccessExpiresAt)}
        mediaAccessUrl={singleParam(params.mediaAccessUrl)}
      />
    </AdminShell>
  );
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
