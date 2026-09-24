import { AdminAuthPanel } from "@/components/AuthPanel";
import { AdminShell } from "@/components/AdminShell";
import { requireAdminPageContext } from "@/lib/admin-page";
import { loadAdminPrivacyData } from "@/lib/dashboard-data";

type PageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function PrivacyPage({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const { session, data } = await requireAdminPageContext();
  if (!session || !data) {
    return <AdminAuthPanel error={singleParam(params.authError)} />;
  }

  const privacyData = await loadAdminPrivacyData(session);

  return (
    <AdminShell>
      <header className="admin-topbar">
        <div>
          <p className="muted">数据源：{privacyData.source}</p>
          <h1 className="admin-title">隐私请求</h1>
        </div>
      </header>

      <section className="admin-grid" aria-label="隐私请求">
        <article className="panel span-12">
          <div className="section-header">
            <h2>请求队列</h2>
            <span className="pill pill-blue">{privacyData.privacyQueue.length} 条请求</span>
          </div>
          {privacyData.privacyQueue.length > 0 ? (
            <div className="queue-list">
              {privacyData.privacyQueue.map((request) => (
                <div className="queue-row" key={request.id}>
                  <div>
                    <strong>{request.type}</strong>
                    <p className="muted queue-row-meta">
                      {request.requesterName} · {request.createdAt}
                    </p>
                  </div>
                  <span className="pill pill-blue">{request.status}</span>
                </div>
              ))}
            </div>
          ) : (
            <p className="empty-state">暂无待处理隐私请求。</p>
          )}
        </article>
      </section>
    </AdminShell>
  );
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
