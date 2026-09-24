import { AlertTriangle, ExternalLink } from "lucide-react";
import { AdminAuthPanel } from "@/components/AuthPanel";
import { AdminShell } from "@/components/AdminShell";
import { requireAdminPageContext } from "@/lib/admin-page";
import { getAdminRuntimeConfig } from "@/lib/runtime";

type PageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function WorkflowsPage({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const { session, data } = await requireAdminPageContext();
  if (!session || !data) {
    return <AdminAuthPanel error={singleParam(params.authError)} />;
  }

  const config = getAdminRuntimeConfig();

  return (
    <AdminShell>
      <header className="admin-topbar">
        <div>
          <p className="muted">数据源：{data.source}</p>
          <h1 className="admin-title">工作流</h1>
        </div>
      </header>

      <section className="admin-grid" aria-label="工作流">
        <article className="panel span-6">
          <div className="section-header">
            <h2>Temporal UI</h2>
            <a className="button-secondary" href={config.temporalUiUrl} rel="noreferrer" target="_blank">
              <ExternalLink size={18} aria-hidden="true" />
              打开入口
            </a>
          </div>
          <p className="muted">工作流控制台</p>
          <strong>{config.temporalUiUrl}</strong>
        </article>

        <article className="panel span-6">
          <h2>运维提示</h2>
          <p className="muted workflow-note">
            <AlertTriangle size={16} aria-hidden="true" />
            失败任务需要保留人工介入入口。
          </p>
        </article>
      </section>
    </AdminShell>
  );
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
