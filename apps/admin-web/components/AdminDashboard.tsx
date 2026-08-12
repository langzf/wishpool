import { AlertTriangle, Archive, Bot, Clock3, Database, RefreshCw, ShieldCheck, Wifi } from "lucide-react";
import { getAdminRuntimeConfig } from "@/lib/runtime";
import type { AdminDashboardData, AdminQueueRow } from "@/lib/dashboard-data";

function QueueIcon({ queue }: Readonly<{ queue: AdminQueueRow }>) {
  if (queue.name.includes("media")) return <Archive size={18} aria-hidden="true" />;
  if (queue.name.includes("ai")) return <Bot size={18} aria-hidden="true" />;
  if (queue.name.includes("privacy")) return <ShieldCheck size={18} aria-hidden="true" />;
  return <RefreshCw size={18} aria-hidden="true" />;
}

export function AdminDashboard({ data }: Readonly<{ data: AdminDashboardData }>) {
  const config = getAdminRuntimeConfig();

  return (
    <>
      <header className="admin-topbar" id="overview">
        <div>
          <p className="muted">本地部署 · {data.familyName}</p>
          <h1 className="admin-title">系统治理</h1>
        </div>
        <div className="admin-actions">
          <button className="button-secondary" type="button">
            <Wifi size={18} aria-hidden="true" />
            健康检查
          </button>
          <button className="button" type="button">
            <RefreshCw size={18} aria-hidden="true" />
            刷新状态
          </button>
        </div>
      </header>

      <section className="admin-grid" aria-label="管理后台总览">
        <article className="panel metric span-3">
          <ShieldCheck size={22} color="#059669" aria-hidden="true" />
          <strong>{data.activeChildren}</strong>
          <span>活跃儿童档案</span>
          <span className="muted">家庭空间正常。</span>
        </article>
        <article className="panel metric span-3">
          <Clock3 size={22} color="#D97706" aria-hidden="true" />
          <strong>{data.pendingReviews}</strong>
          <span>待处理审核</span>
          <span className="muted">家长端可直接处理。</span>
        </article>
        <article className="panel metric span-3">
          <Bot size={22} color="#2563EB" aria-hidden="true" />
          <strong>4</strong>
          <span>AI 能力接口</span>
          <span className="muted">预审、反馈、纪念册、隐私摘要。</span>
        </article>
        <article className="panel metric span-3">
          <Database size={22} color="#2563EB" aria-hidden="true" />
          <strong>{data.migrationCount}</strong>
          <span>数据库迁移</span>
          <span className="muted">Flyway 校验通过。</span>
        </article>

        <article className="panel span-7">
          <h2>服务健康</h2>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>服务</th>
                  <th>状态</th>
                  <th>延迟/说明</th>
                  <th>入口</th>
                </tr>
              </thead>
              <tbody>
                {data.serviceHealth.map((service) => (
                  <tr key={service.name}>
                    <td>{service.name}</td>
                    <td>
                      <span className={service.status === "healthy" ? "pill pill-green" : "pill pill-blue"}>
                        {service.status}
                      </span>
                    </td>
                    <td>{service.latency}</td>
                    <td>{service.entry ?? (service.name === "Core API" ? config.coreApiBaseUrl : "-")}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </article>

        <article className="panel span-5" id="queues">
          <h2>异步队列</h2>
          <div className="queue-list">
            {data.queues.map((queue) => (
              <div className="queue-row" key={queue.name}>
                <div>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <QueueIcon queue={queue} />
                    <strong>{queue.name}</strong>
                  </div>
                  <p className="muted" style={{ margin: "6px 0 0" }}>
                    pending {queue.pending} · retrying {queue.retrying}
                  </p>
                </div>
                <span className={queue.pending > 0 ? "pill pill-amber" : "pill pill-green"}>
                  {queue.pending > 0 ? "观察" : "清空"}
                </span>
              </div>
            ))}
          </div>
        </article>

        <article className="panel span-4" id="privacy">
          <h2>隐私请求</h2>
          <div className="queue-list">
            {data.privacyQueue.map((request) => (
              <div className="queue-row" key={request.id}>
                <div>
                  <strong>{request.type}</strong>
                  <p className="muted" style={{ margin: "6px 0 0" }}>
                    {request.requesterName} · {request.createdAt}
                  </p>
                </div>
                <span className="pill pill-blue">{request.status}</span>
              </div>
            ))}
          </div>
        </article>

        <article className="panel span-4" id="storage">
          <h2>对象存储</h2>
          <p className="muted">MinIO 控制台</p>
          <strong>{config.minioConsoleUrl}</strong>
          <p className="muted">媒体处理完成率</p>
          <div className="bar-track" aria-label="媒体处理完成率 94%">
            <div className="bar-fill" style={{ width: "94%" }} />
          </div>
        </article>

        <article className="panel span-4" id="settings">
          <h2>工作流</h2>
          <p className="muted">Temporal UI</p>
          <strong>{config.temporalUiUrl}</strong>
          <p className="muted" style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <AlertTriangle size={16} aria-hidden="true" />
            失败任务需要保留人工介入入口。
          </p>
        </article>

        <article className="panel span-12" id="audit">
          <h2>审计轨迹</h2>
          <p className="muted">数据源：{data.source}</p>
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
                {data.auditRows.map((row) => (
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
        </article>
      </section>
    </>
  );
}
