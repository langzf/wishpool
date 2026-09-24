import Link from "next/link";
import {
  Archive,
  Bot,
  Clock3,
  Database,
  FileClock,
  GitBranch,
  Home,
  LockKeyhole,
  RefreshCw,
  Shield,
  ShieldCheck,
  Wifi
} from "lucide-react";
import { logoutAdminAction } from "@/app/actions";
import type { AdminDashboardData } from "@/lib/dashboard-data";
import { getAdminRuntimeConfig } from "@/lib/runtime";

const modules = [
  { href: "/families", title: "家庭", description: "查看家庭状态、儿童档案和成员规模。", icon: Home },
  { href: "/privacy", title: "隐私", description: "跟踪导出、删除等隐私请求处理进度。", icon: LockKeyhole },
  { href: "/audit", title: "审计", description: "检索管理操作和系统事件轨迹。", icon: Shield },
  { href: "/queues", title: "队列", description: "观察异步任务积压和重试状态。", icon: FileClock },
  { href: "/storage", title: "存储", description: "进入对象存储并创建媒体排障授权。", icon: Archive },
  { href: "/workflows", title: "工作流", description: "打开 Temporal 运维入口处理失败任务。", icon: GitBranch }
];

export function AdminDashboard({
  data
}: Readonly<{
  data: AdminDashboardData;
  mediaAccessAuditLogId?: string;
  mediaAccessExpiresAt?: string;
  mediaAccessUrl?: string;
}>) {
  const config = getAdminRuntimeConfig();

  return (
    <>
      <header className="admin-topbar" id="overview">
        <div>
          <p className="muted">本地部署 · {data.familyName}</p>
          <h1 className="admin-title">系统治理</h1>
        </div>
        <div className="admin-actions">
          <a className="button-secondary" href={`${config.adminApiBaseUrl}/health`} rel="noreferrer" target="_blank">
            <Wifi size={18} aria-hidden="true" />
            健康检查
          </a>
          <Link className="button" href="/">
            <RefreshCw size={18} aria-hidden="true" />
            刷新状态
          </Link>
          <form action={logoutAdminAction}>
            <button className="button-secondary" type="submit">
              退出
            </button>
          </form>
        </div>
      </header>

      <section className="admin-grid" aria-label="管理后台总览">
        <article className="panel metric span-3">
          <span className="metric-icon metric-icon-green" aria-hidden="true">
            <ShieldCheck size={22} />
          </span>
          <strong>{data.activeChildren}</strong>
          <span>活跃儿童档案</span>
          <span className="muted">家庭空间正常。</span>
        </article>
        <article className="panel metric span-3">
          <span className="metric-icon metric-icon-amber" aria-hidden="true">
            <Clock3 size={22} />
          </span>
          <strong>{data.pendingReviews}</strong>
          <span>待处理审核</span>
          <span className="muted">家长端可直接处理。</span>
        </article>
        <article className="panel metric span-3">
          <span className="metric-icon metric-icon-blue" aria-hidden="true">
            <Bot size={22} />
          </span>
          <strong>4</strong>
          <span>AI 能力接口</span>
          <span className="muted">预审、反馈、纪念册、隐私摘要。</span>
        </article>
        <article className="panel metric span-3">
          <span className="metric-icon metric-icon-violet" aria-hidden="true">
            <Database size={22} />
          </span>
          <strong>{data.migrationCount}</strong>
          <span>数据库迁移</span>
          <span className="muted">Flyway 校验通过。</span>
        </article>

        <article className="panel span-7">
          <div className="section-header">
            <div>
              <h2>服务健康</h2>
              <p className="muted">数据源：{data.source}</p>
            </div>
          </div>
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

        <section className="span-5 module-card-grid" aria-label="模块入口">
          {modules.map((module) => {
            const Icon = module.icon;
            return (
              <Link className="panel module-card" href={module.href} key={module.href}>
                <span className="module-card-icon" aria-hidden="true">
                  <Icon size={20} />
                </span>
                <strong>{module.title}</strong>
                <span className="muted">{module.description}</span>
              </Link>
            );
          })}
        </section>
      </section>
    </>
  );
}
