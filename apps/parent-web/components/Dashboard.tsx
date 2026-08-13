import {
  ArrowRight,
  CalendarDays,
  CheckCircle2,
  CircleDot,
  Clock3,
  Gift,
  Image,
  Bell,
  Mic,
  Radio,
  ShieldCheck,
  SkipForward,
  Video
} from "lucide-react";
import {
  approveReviewAction,
  arrangeRoomItemAction,
  createPairingSessionAction,
  createWishAction,
  exportMemoryAction,
  logoutParentAction,
  markNotificationsReadAction,
  postponeTaskAction,
  requestRevisionAction,
  saveWeeklyPlanAction,
  skipTaskAction,
  updateNotificationPreferenceAction
} from "@/app/actions";
import { getApiRuntimeConfig } from "@/lib/api";
import type { ParentDashboardData } from "@/lib/dashboard-data";
import { RealtimeRefresh } from "@/components/RealtimeRefresh";

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

export function ParentDashboard({
  data,
  pairingCode,
  pairingExpiresAt
}: Readonly<{ data: ParentDashboardData; pairingCode?: string; pairingExpiresAt?: string }>) {
  const config = getApiRuntimeConfig();
  const targetFragments = Math.max(data.currentWish.targetFragments, 1);
  const wishProgress = Math.round((data.currentWish.currentFragments / targetFragments) * 100);
  const planRulesJson = JSON.stringify(
    data.weeklyPlan.rules.map((rule, index) => ({
      title: rule.title,
      category: rule.category,
      submissionType: rule.submissionType,
      weekdays: rule.weekdays,
      isCore: rule.isCore,
      requireReview: rule.requireReview,
      sortOrder: index
    }))
  );

  return (
    <>
      <RealtimeRefresh enabled={data.source === "api"} />
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
          <a className="primary-button" href="#plan">
            <CalendarDays size={18} aria-hidden="true" />
            安排本周
          </a>
          <form action={logoutParentAction}>
            <button className="secondary-button" type="submit">
              退出
            </button>
          </form>
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
            <a className="icon-button" aria-label="查看全部审核" href="#reviews">
              <ArrowRight size={18} aria-hidden="true" />
            </a>
          </div>
          <div className="review-list">
            {data.pendingReviews.length === 0 ? (
              <div className="empty-state">
                <CheckCircle2 size={20} aria-hidden="true" />
                <div>
                  <strong>没有待审核提交</strong>
                  <p className="muted">孩子提交任务后会出现在这里，并通过实时连接刷新。</p>
                </div>
              </div>
            ) : (
              data.pendingReviews.map((review) => (
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
                  <div className="review-actions">
                    <form action={requestRevisionAction}>
                      <input name="submissionId" type="hidden" value={review.submissionId} />
                      <input name="feedbackText" type="hidden" value="请再补充一点，让记录更完整。" />
                      <button className="secondary-button" type="submit">
                        退回
                      </button>
                    </form>
                    <form action={approveReviewAction}>
                      <input name="submissionId" type="hidden" value={review.submissionId} />
                      <input name="feedbackText" type="hidden" value="看到了，完成得很好。" />
                      <button className="primary-button" type="submit">
                        通过
                      </button>
                    </form>
                  </div>
                </div>
              ))
            )}
          </div>
        </article>

        <article className="panel span-5">
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
                      星光 +{task.rewardStarlight} · {task.submissionType}
                    </p>
                  </div>
                  <div className="task-actions">
                    <span className={`status-pill ${status.className}`}>{status.label}</span>
                    {open ? (
                      <>
                        <form action={skipTaskAction}>
                          <input name="taskId" type="hidden" value={task.id} />
                          <input name="reason" type="hidden" value="家庭临时调整" />
                          <button className="icon-button" aria-label={`${task.title} 跳过`} type="submit">
                            <SkipForward size={16} aria-hidden="true" />
                          </button>
                        </form>
                        <form action={postponeTaskAction}>
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

        <article className="panel span-4" id="wish">
          <h2>心愿卡</h2>
          <p className="muted">{data.currentWish.description}</p>
          <div className="progress-track" aria-label={`心愿进度 ${wishProgress}%`}>
            <div className="progress-fill" style={{ width: `${wishProgress}%` }} />
          </div>
          <p>
            <strong>{data.currentWish.currentFragments}</strong> / {data.currentWish.targetFragments} 心愿碎片
          </p>
          {data.source === "api" ? (
            <form action={createWishAction} className="wish-form">
              <input name="childId" type="hidden" value={data.child.id} />
              <input name="weekId" type="hidden" value={data.weeklyPlan.weekId} />
              <input name="rewardMode" type="hidden" value={data.weeklyPlan.rewardMode} />
              <label>
                新心愿
                <input name="title" placeholder="周末公园野餐" />
              </label>
              <label>
                说明
                <input name="note" placeholder="完成本周计划后兑现" />
              </label>
              <label>
                碎片
                <input defaultValue="10" min="1" name="requiredFragments" type="number" />
              </label>
              <button className="primary-button" type="submit">
                创建并激活
              </button>
            </form>
          ) : null}
        </article>

        <article className="panel span-4" id="plan">
          <h2>本周计划</h2>
          <div className="task-list">
            {data.weeklyPlan.rules.length === 0 ? (
              <div className="empty-state">
                <CalendarDays size={20} aria-hidden="true" />
                <div>
                  <strong>还没有本周计划</strong>
                  <p className="muted">先创建任务模板并保存周计划，今日任务会随之生成。</p>
                </div>
              </div>
            ) : (
              data.weeklyPlan.rules.map((rule) => (
                <div className="task-row" key={rule.title}>
                  <strong>{rule.title}</strong>
                  <span className="status-pill status-blue">{rule.weekdays.length} 天</span>
                </div>
              ))
            )}
          </div>
          {data.weeklyPlan.rules.length > 0 ? (
            <form action={saveWeeklyPlanAction} className="plan-actions">
              <input name="childId" type="hidden" value={data.weeklyPlan.childId} />
              <input name="weekId" type="hidden" value={data.weeklyPlan.weekId} />
              <input name="startDate" type="hidden" value={data.weeklyPlan.weekStartDate} />
              <input name="endDate" type="hidden" value={data.weeklyPlan.weekEndDate} />
              <input name="rewardMode" type="hidden" value={data.weeklyPlan.rewardMode} />
              <input name="rulesJson" type="hidden" value={planRulesJson} />
              <button className="primary-button" type="submit">
                保存计划
              </button>
            </form>
          ) : null}
        </article>

        <article className="panel span-4">
          <h2>成长纪念册</h2>
          <div className="memory-list">
            {data.memories.length === 0 ? (
              <div className="empty-state">
                <Clock3 size={20} aria-hidden="true" />
                <div>
                  <strong>还没有成长周卡</strong>
                  <p className="muted">周末生成纪念册后，回顾和导出入口会出现在这里。</p>
                </div>
              </div>
            ) : (
              data.memories.map((memory) => (
                <div className="memory-row" key={memory.id}>
                  <div>
                    <strong>{memory.title}</strong>
                    <p className="muted" style={{ margin: "6px 0 0" }}>
                      {memory.summary}
                    </p>
                  </div>
                  {data.source === "api" ? (
                    <form action={exportMemoryAction}>
                      <input name="memoryId" type="hidden" value={memory.id} />
                      <input name="format" type="hidden" value="pdf" />
                      <button className="secondary-button" type="submit">
                        导出
                      </button>
                    </form>
                  ) : null}
                </div>
              ))
            )}
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
                {data.source === "api" ? (
                  <form action={arrangeRoomItemAction}>
                    <input name="itemId" type="hidden" value={item.id} />
                    <input name="x" type="hidden" value={nextRoomX(item.x)} />
                    <input name="y" type="hidden" value={nextRoomY(item.y)} />
                    <button aria-label={`${item.name} 移动摆放`} className="mini-icon-button" type="submit">
                      <ArrowRight size={14} aria-hidden="true" />
                    </button>
                  </form>
                ) : null}
              </div>
            ))}
          </div>
        </article>

        <article className="panel span-4" id="notifications">
          <h2>通知中心</h2>
          <div className="notification-list">
            {data.notificationInbox.length === 0 ? (
              <div className="empty-state">
                <Bell size={20} aria-hidden="true" />
                <div>
                  <strong>没有站内通知</strong>
                  <p className="muted">审核、任务变更、心愿解锁等提醒会汇总到这里。</p>
                </div>
              </div>
            ) : (
              data.notificationInbox.map((notification) => (
                <div className="notification-row" key={notification.id}>
                  <div>
                    <strong>{notification.title}</strong>
                    <p className="muted" style={{ margin: "6px 0 0" }}>
                      {notification.body}
                    </p>
                  </div>
                  <span className={`status-pill ${notification.status === "read" ? "status-ready" : "status-waiting"}`}>
                    {notification.status === "read" ? "已读" : "未读"}
                  </span>
                </div>
              ))
            )}
          </div>
          {data.source === "api" && data.notificationInbox.some((notification) => notification.status !== "read") ? (
            <form action={markNotificationsReadAction} className="plan-actions">
              {data.notificationInbox
                .filter((notification) => notification.status !== "read")
                .map((notification) => (
                  <input key={notification.id} name="notificationIds" type="hidden" value={notification.id} />
                ))}
              <button className="secondary-button" type="submit">
                <Bell size={16} aria-hidden="true" />
                全部已读
              </button>
            </form>
          ) : null}
        </article>

        <article className="panel span-4">
          <h2>通知偏好</h2>
          <div className="preference-list">
            {data.notificationPreferences.map((preference) => (
              <form action={updateNotificationPreferenceAction} className="preference-row" key={preference.notificationType}>
                <input name="notificationType" type="hidden" value={preference.notificationType} />
                <input name="enabled" type="hidden" value={preference.enabled ? "false" : "true"} />
                <span>{notificationLabel(preference.notificationType)}</span>
                <button className={preference.enabled ? "primary-button" : "secondary-button"} type="submit">
                  {preference.enabled ? "开启" : "关闭"}
                </button>
              </form>
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
          <form action={createPairingSessionAction} className="plan-actions">
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
          <p className="muted" style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <Clock3 size={16} aria-hidden="true" />
            {data.source === "api" ? "已通过聚合接口读取业务数据。" : "未配置访问令牌时显示共享样例数据。"}
          </p>
        </article>
      </section>
    </>
  );
}

function notificationLabel(type: string) {
  const labels: Record<string, string> = {
    child_submission_created: "孩子提交提醒",
    ai_precheck_completed: "AI 预审提醒",
    review_completed: "审核反馈提醒",
    wish_fragment_earned: "心愿碎片提醒",
    wish_unlocked: "心愿解锁提醒",
    wish_redeemed_memory_generated: "纪念册生成提醒",
    task_plan_changed: "任务变更提醒"
  };
  return labels[type] ?? "家庭提醒";
}

function nextDate(date: string) {
  const parsed = new Date(`${date}T00:00:00Z`);
  if (Number.isNaN(parsed.getTime())) return date;
  parsed.setUTCDate(parsed.getUTCDate() + 1);
  return parsed.toISOString().slice(0, 10);
}

function nextRoomX(value: number) {
  return Math.round((value + 12) % 84);
}

function nextRoomY(value: number) {
  return Math.round(value > 70 ? 28 : value + 8);
}
