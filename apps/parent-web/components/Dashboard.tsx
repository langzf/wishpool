import {
  ArrowRight,
  CalendarDays,
  CheckCircle2,
  CircleDot,
  Clock3,
  Gift,
  Image,
  Mic,
  Radio,
  ShieldCheck,
  Video
} from "lucide-react";
import { getApiRuntimeConfig } from "@/lib/api";
import type { ParentDashboardData } from "@/lib/dashboard-data";

const statusLabel: Record<string, { label: string; className: string }> = {
  todo: { label: "待打卡", className: "status-blue" },
  pending_review: { label: "待审核", className: "status-waiting" },
  approved: { label: "已通过", className: "status-ready" }
};

function MediaIcon({ type }: Readonly<{ type: string }>) {
  if (type === "audio") return <Mic size={18} aria-hidden="true" />;
  if (type === "video") return <Video size={18} aria-hidden="true" />;
  return <Image size={18} aria-hidden="true" />;
}

export function ParentDashboard({ data }: Readonly<{ data: ParentDashboardData }>) {
  const config = getApiRuntimeConfig();
  const wishProgress = Math.round((data.currentWish.currentFragments / data.currentWish.targetFragments) * 100);

  return (
    <>
      <header className="topbar" id="overview">
        <div>
          <p className="muted">今天好，{data.child.nickname} 的成长小屋正在发光</p>
          <h1 className="page-title">家长工作台</h1>
        </div>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <button className="secondary-button" type="button">
            <Radio size={18} aria-hidden="true" />
            实时 {config.realtimeBaseUrl.includes("8090") ? "本地" : "已配置"}
          </button>
          <button className="primary-button" type="button">
            <CalendarDays size={18} aria-hidden="true" />
            安排本周
          </button>
        </div>
      </header>

      <section className="dashboard-grid" aria-label="家长工作台概览">
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
          <CircleDot size={22} color="#D97706" aria-hidden="true" />
          <span className="metric-value">{data.dashboardMetrics.starlightIssuedThisWeek}</span>
          <strong>本周星光</strong>
          <span className="muted">审核通过后自动入账。</span>
        </article>
        <article className="panel metric span-3">
          <Gift size={22} color="#2563EB" aria-hidden="true" />
          <span className="metric-value">{wishProgress}%</span>
          <strong>心愿进度</strong>
          <span className="muted">{data.currentWish.title}</span>
        </article>

        <article className="panel span-7" id="reviews">
          <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center" }}>
            <h2>待审核</h2>
            <button className="icon-button" aria-label="查看全部审核" type="button">
              <ArrowRight size={18} aria-hidden="true" />
            </button>
          </div>
          <div className="review-list">
            {data.pendingReviews.map((review) => (
              <div className="review-row" key={review.submissionId}>
                <div>
                  <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
                    <MediaIcon type={review.mediaType} />
                    <strong>{review.taskTitle}</strong>
                    <span className="status-pill status-waiting">待确认</span>
                  </div>
                  <p className="muted" style={{ margin: "8px 0 0" }}>
                    {review.aiSummary}
                  </p>
                </div>
                <button className="primary-button" type="button">
                  审核
                </button>
              </div>
            ))}
          </div>
        </article>

        <article className="panel span-5">
          <h2>今日任务</h2>
          <div className="task-list">
            {data.todayTasks.map((task) => {
              const status = statusLabel[task.status] ?? statusLabel.todo;
              return (
                <div className="task-row" key={task.id}>
                  <div>
                    <strong>{task.title}</strong>
                    <p className="muted" style={{ margin: "6px 0 0" }}>
                      星光 +{task.rewardStarlight} · {task.submissionType}
                    </p>
                  </div>
                  <span className={`status-pill ${status.className}`}>{status.label}</span>
                </div>
              );
            })}
          </div>
        </article>

        <article className="panel span-4" id="wish">
          <h2>心愿卡</h2>
          <p className="muted">{data.currentWish.description}</p>
          <div className="progress-track" aria-label={`心愿进度 ${wishProgress}%`}>
            <div className="progress-fill" style={{ width: `${wishProgress}%` }} />
          </div>
          <p>
            <strong>{data.currentWish.currentFragments}</strong> / {data.currentWish.targetFragments} 心愿碎片
          </p>
        </article>

        <article className="panel span-4" id="plan">
          <h2>本周计划</h2>
          <div className="task-list">
            {data.weeklyPlan.rules.map((rule) => (
              <div className="task-row" key={rule.title}>
                <strong>{rule.title}</strong>
                <span className="status-pill status-blue">{rule.weekdays.length} 天</span>
              </div>
            ))}
          </div>
        </article>

        <article className="panel span-4">
          <h2>成长纪念册</h2>
          <div className="memory-list">
            {data.memories.map((memory) => (
              <div className="memory-row" key={memory.id}>
                <div>
                  <strong>{memory.title}</strong>
                  <p className="muted" style={{ margin: "6px 0 0" }}>
                    {memory.summary}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </article>

        <article className="panel span-8" id="room">
          <h2>小屋摆放预览</h2>
          <div className="room-preview" aria-label={`${data.child.nickname} 的小屋`}>
            {data.roomState.items.map((item) => (
              <div
                className="room-item"
                key={item.id}
                style={{ left: `${item.x}%`, top: `${item.y}%`, opacity: item.unlocked ? 1 : 0.62 }}
              >
                <Gift size={16} aria-hidden="true" />
                {item.name}
              </div>
            ))}
          </div>
        </article>

        <article className="panel span-4" id="settings">
          <h2>本地服务</h2>
          <p className="muted">Core API</p>
          <strong>{config.coreApiBaseUrl}</strong>
          <p className="muted">媒体处理就绪率</p>
          <strong>{Math.round(data.dashboardMetrics.mediaProcessingReadyRate * 100)}%</strong>
          <p className="muted">数据源</p>
          <strong>{data.source === "api" && data.coreApiHealthy ? "Core API" : "样例兜底"}</strong>
          <p className="muted" style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <Clock3 size={16} aria-hidden="true" />
            数据来自共享样例，真实源切换点已预留。
          </p>
        </article>
      </section>
    </>
  );
}
