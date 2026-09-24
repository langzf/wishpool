import { AdminAuthPanel } from "@/components/AuthPanel";
import { AdminShell } from "@/components/AdminShell";
import { requireAdminPageContext } from "@/lib/admin-page";
import { loadAdminFamiliesData } from "@/lib/dashboard-data";

type PageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function FamiliesPage({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const { session, data } = await requireAdminPageContext();
  if (!session || !data) {
    return <AdminAuthPanel error={singleParam(params.authError)} />;
  }

  const familiesData = await loadAdminFamiliesData(session);

  return (
    <AdminShell>
      <header className="admin-topbar">
        <div>
          <p className="muted">数据源：{familiesData.source}</p>
          <h1 className="admin-title">家庭管理</h1>
        </div>
      </header>

      <section className="admin-grid" aria-label="家庭管理">
        <article className="panel span-12">
          <div className="section-header">
            <h2>家庭列表</h2>
            <span className="pill pill-blue">{familiesData.families.length} 个家庭</span>
          </div>
          {familiesData.families.length > 0 ? (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>家庭名</th>
                    <th>状态</th>
                    <th>儿童数</th>
                    <th>成员数</th>
                    <th>时区</th>
                    <th>创建时间</th>
                  </tr>
                </thead>
                <tbody>
                  {familiesData.families.map((row) => (
                    <tr key={row.id}>
                      <td>{row.name}</td>
                      <td>
                        <span className={row.status === "active" ? "pill pill-green" : "pill pill-blue"}>
                          {row.status}
                        </span>
                      </td>
                      <td>{row.childCount}</td>
                      <td>{row.memberCount}</td>
                      <td>{row.timezone}</td>
                      <td>{row.createdAt}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="empty-state">暂无家庭数据。</p>
          )}
        </article>
      </section>
    </AdminShell>
  );
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
