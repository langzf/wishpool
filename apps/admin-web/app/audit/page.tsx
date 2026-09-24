import { AdminAuthPanel } from "@/components/AuthPanel";
import { AdminShell } from "@/components/AdminShell";
import { requireAdminPageContext } from "@/lib/admin-page";
import { loadAdminAuditData } from "@/lib/dashboard-data";

type PageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function AuditPage({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const { session, data } = await requireAdminPageContext();
  if (!session || !data) {
    return <AdminAuthPanel error={singleParam(params.authError)} />;
  }

  const auditData = await loadAdminAuditData(session);

  return (
    <AdminShell>
      <header className="admin-topbar">
        <div>
          <p className="muted">数据源：{auditData.source}</p>
          <h1 className="admin-title">审计轨迹</h1>
        </div>
      </header>

      <section className="admin-grid" aria-label="审计轨迹">
        <article className="panel span-12">
          <div className="section-header">
            <h2>审计日志</h2>
            <span className="pill pill-blue">{auditData.auditRows.length} 条记录</span>
          </div>
          {auditData.auditRows.length > 0 ? (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>时间</th>
                    <th>操作者</th>
                    <th>动作</th>
                    <th>对象</th>
                  </tr>
                </thead>
                <tbody>
                  {auditData.auditRows.map((row) => (
                    <tr key={row.id}>
                      <td>{row.time}</td>
                      <td>{row.actor}</td>
                      <td>{row.action}</td>
                      <td>{row.target}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="empty-state">暂无审计日志。</p>
          )}
        </article>
      </section>
    </AdminShell>
  );
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
