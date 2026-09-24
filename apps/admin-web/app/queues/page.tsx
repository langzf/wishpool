import { Archive, Bot, RefreshCw, ShieldCheck } from "lucide-react";
import { AdminAuthPanel } from "@/components/AuthPanel";
import { AdminShell } from "@/components/AdminShell";
import { requireAdminPageContext } from "@/lib/admin-page";
import { loadAdminQueuesData, type AdminQueueRow } from "@/lib/dashboard-data";

type PageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function QueuesPage({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const { session, data } = await requireAdminPageContext();
  if (!session || !data) {
    return <AdminAuthPanel error={singleParam(params.authError)} />;
  }

  const queuesData = loadAdminQueuesData(data);

  return (
    <AdminShell>
      <header className="admin-topbar">
        <div>
          <p className="muted">数据源：{queuesData.source}</p>
          <h1 className="admin-title">异步队列</h1>
        </div>
      </header>

      <section className="admin-grid" aria-label="异步队列">
        <article className="panel span-12">
          <div className="section-header">
            <h2>任务队列</h2>
            <span className="pill pill-blue">{queuesData.queues.length} 个队列</span>
          </div>
          {queuesData.queues.length > 0 ? (
            <div className="queue-list">
              {queuesData.queues.map((queue) => (
                <div className="queue-row" key={queue.name}>
                  <div>
                    <div className="queue-row-title">
                      <QueueIcon queue={queue} />
                      <strong>{queue.name}</strong>
                    </div>
                    <p className="muted queue-row-meta">
                      pending {queue.pending} · retrying {queue.retrying}
                    </p>
                  </div>
                  <span className={queue.pending > 0 ? "pill pill-amber" : "pill pill-green"}>
                    {queue.pending > 0 ? "观察" : "清空"}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <p className="empty-state">暂无队列数据。</p>
          )}
        </article>
      </section>
    </AdminShell>
  );
}

function QueueIcon({ queue }: Readonly<{ queue: AdminQueueRow }>) {
  if (queue.name.includes("media")) return <Archive size={18} aria-hidden="true" />;
  if (queue.name.includes("ai")) return <Bot size={18} aria-hidden="true" />;
  if (queue.name.includes("privacy")) return <ShieldCheck size={18} aria-hidden="true" />;
  return <RefreshCw size={18} aria-hidden="true" />;
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
