import Link from "next/link";
import {
  Bell,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Gift,
  Heart,
  Home,
  Images,
  Radio,
  Sparkles,
  ShieldCheck,
  SkipForward,
} from "lucide-react";
import { createPairingSessionAction, logoutParentAction, postponeTaskAction, skipTaskAction } from "@/app/actions";
import { FragmentGrid } from "@/components/FragmentGrid";
import { getApiRuntimeConfig } from "@/lib/api";
import type { ParentDashboardData } from "@/lib/dashboard-data";

const statusLabel: Record<string, { label: string; className: string }> = {
  todo: { label: "待打卡", className: "status-blue" },
  needs_revision: { label: "需修改", className: "status-waiting" },
  adjusted_by_parent: { label: "已调整", className: "status-blue" },
  pending_review: { label: "待审核", className: "status-waiting" },
  approved: { label: "已通过", className: "status-ready" },
  rejected: { label: "已退回", className: "status-waiting" },
  skipped: { label: "已跳过", className: "status-ready" },
  postponed: { label: "已延后", className: "status-blue" }
};

const moduleCards = [
  { href: "/reviews", title: "审核", description: "处理孩子提交和 AI 预审结果。", icon: ShieldCheck },
  { href: "/plan", title: "计划", description: "安排模板、本周规则和今日任务。", icon: BookOpen },
  { href: "/wish", title: "心愿", description: "查看碎片进度并设置新心愿。", icon: Heart },
  { href: "/memories", title: "纪念册", description: "回顾成长周卡和导出记录。", icon: Images },
  { href: "/room", title: "小屋", description: "查看已解锁物件和摆放状态。", icon: Home },
  { href: "/notifications", title: "通知", description: "管理提醒、站内信和偏好。", icon: Bell }
];

export function ParentDashboard({
  data,
  pairingCode,
  pairingExpiresAt,
  actionError,
  actionSuccess
}: Readonly<{ data: ParentDashboardData; pairingCode?: string; pairingExpiresAt?: string; actionError?: string; actionSuccess?: string }>) {
  const config = getApiRuntimeConfig();
  const targetFragments = Math.max(data.currentWish.targetFragments, 1);
  const wishProgress = Math.round((data.currentWish.currentFragments / targetFragments) * 100);

  return (
    <>
      <header className="topbar" id="overview">
        <div>
          <p className="muted">今天好，{data.child.nickname} 的成长小屋正在发光</p>
          <h1 className="page-title">家长工作台</h1>
        </div>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <span className="secondary-button" role="status">
            <Radio size={18} aria-hidden="true" />
            实时 {config.realtimeBaseUrl.includes("8090") ? "本地" : "已配置"}
          </span>
          <Link className="primary-button" href="/plan">
            <CalendarDays size={18} aria-hidden="true" />
            安排本周
          </Link>
          <form action={logoutParentAction}>
            <button className="secondary-button" type="submit">
              退出
            </button>
          </form>
        </div>
      </header>

      <section className="dashboard-grid" aria-label="家长工作台概览">
        {actionError ? <p aria-live="assertive" className="form-error span-12" role="alert">{actionError}</p> : null}
        {actionSuccess ? <p aria-live="polite" className="form-success span-12" role="status">{actionSuccess}</p> : null}
        <article className="panel metric span-3">
          <ShieldCheck size={22} color="#2563EB" aria-hidden="true" />
          <span className="metric-value">{data.dashboardMetrics.pendingReviews}</span>
          <strong>待审核提交</strong>
          <span className="muted">AI 已完成摘要，适合快速处理。</span>
        </article>
        <article className="panel metric span-3">
          <CheckCircle2 size={22} color="#059669" aria-hidden="true" />
          <span className="metric-value">{Math.round(data.dashboardMetrics.weeklyCompletionRate * 100)}%</span>
          <strong>本周完成率</strong>
          <span className="muted">核心任务保持稳定节奏。</span>
        </article>
        <article className="panel metric span-3">
          <Gift size={22} color="#2563EB" aria-hidden="true" />
          <span className="metric-value">{wishProgress}%</span>
          <strong>心愿进度</strong>
          <span className="muted">{data.currentWish.title}</span>
        </article>

        <article className="panel metric span-3">
          <Sparkles size={22} color="#D97706" aria-hidden="true" />
          <span className="metric-value">{data.rewardSummary ? data.rewardSummary.starLight : "未提供"}</span>
          <strong>累计星光</strong>
          <span className="muted">来自奖励流水汇总</span>
        </article>

        <article className="panel span-4">
          <h2>心愿进度</h2>
          <FragmentGrid
            className="dashboard-wish-fragment-grid"
            cols={data.currentWish.fragmentCols}
            current={data.currentWish.currentFragments}
            description={data.currentWish.description}
            imageUrl={data.currentWish.imageUrl}
            litIndexes={data.currentWish.litIndexes}
            mask={data.currentWish.fragmentMask}
            mode={data.currentWish.fragmentVisualMode}
            rows={data.currentWish.fragmentRows}
            target={data.currentWish.targetFragments}
            title={data.currentWish.title}
          />
          <p className="dashboard-wish-progress">
            <strong>{wishProgress}%</strong>
            <span>
              {data.currentWish.currentFragments} / {data.currentWish.targetFragments} 心愿碎片
            </span>
          </p>
        </article>

        <article className="panel span-8">
          <h2>今日任务</h2>
          <div className="task-list">
            {data.todayTasks.length === 0 ? (
              <div className="empty-state">
                <CalendarDays size={20} aria-hidden="true" />
                <div>
                  <strong>今天没有已生成任务</strong>
                  <p className="muted">保存本周计划后，系统会按计划生成当天任务。</p>
                </div>
              </div>
            ) : (
              data.todayTasks.map((task) => {
                const status = statusLabel[task.status] ?? statusLabel.todo;
                const open = task.status === "todo" || task.status === "needs_revision" || task.status === "adjusted_by_parent";
                return (
                  <div className="task-row" key={task.id}>
                    <div>
                      <strong>{task.title}</strong>
                      <p className="muted" style={{ margin: "6px 0 0" }}>
                        {task.submissionType}
                      </p>
                    </div>
                    <div className="task-actions">
                      <span className={`status-pill ${status.className}`}>{status.label}</span>
                      {data.source === "api" && open ? (
                        <>
                          <form action={skipTaskAction}>
                            <input name="returnTo" type="hidden" value="/" />
                            <input name="taskId" type="hidden" value={task.id} />
                            <input name="reason" type="hidden" value="家庭临时调整" />
                            <button className="icon-button" aria-label={`${task.title} 跳过`} type="submit">
                              <SkipForward size={16} aria-hidden="true" />
                            </button>
                          </form>
                          <form action={postponeTaskAction}>
                            <input name="returnTo" type="hidden" value="/" />
                            <input name="taskId" type="hidden" value={task.id} />
                            <input name="newDate" type="hidden" value={nextDate(task.scheduledDate)} />
                            <input name="reason" type="hidden" value="顺延到下一天" />
                            <button className="secondary-button" type="submit">
                              延后
                            </button>
                          </form>
                        </>
                      ) : null}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </article>

        <article className="panel span-8">
          <h2>模块入口</h2>
          <div className="module-card-grid">
            {moduleCards.map((card) => {
              const Icon = card.icon;
              return (
                <Link className="module-card" href={card.href} key={card.href}>
                  <span className="module-card-icon">
                    <Icon size={20} aria-hidden="true" />
                  </span>
                  <span>
                    <strong>{card.title}</strong>
                    <span className="muted">{card.description}</span>
                  </span>
                </Link>
              );
            })}
          </div>
        </article>

        <article className="panel span-4" id="settings">
          <h2>本地服务</h2>
          <p className="muted">Core API</p>
          <strong>{config.coreApiBaseUrl}</strong>
          <p className="muted">媒体处理就绪率</p>
          <strong>{Math.round(data.dashboardMetrics.mediaProcessingReadyRate * 100)}%</strong>
          <p className="muted">数据源</p>
          <strong>{data.source === "fixture" ? "演示数据" : "Core API"}</strong>
          <form action={createPairingSessionAction} className="plan-actions">
            <input name="returnTo" type="hidden" value="/#settings" />
            <input name="childId" type="hidden" value={data.child.id} />
            <button className="secondary-button" type="submit">
              生成儿童配对码
            </button>
          </form>
          {pairingCode ? (
            <p className="pairing-code">
              {pairingCode}
              {pairingExpiresAt ? <span>有效期至 {pairingExpiresAt}</span> : null}
            </p>
          ) : null}
          <p className="muted">
            {data.source === "fixture" ? "当前为显式演示模式，页面使用共享演示数据。" : "已通过聚合接口读取业务数据。"}
          </p>
        </article>
      </section>
    </>
  );
}

function nextDate(date: string) {
  const parsed = new Date(`${date}T00:00:00Z`);
  if (Number.isNaN(parsed.getTime())) return date;
  parsed.setUTCDate(parsed.getUTCDate() + 1);
  return parsed.toISOString().slice(0, 10);
}
