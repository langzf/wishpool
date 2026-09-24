import { BarChart3, CalendarDays, Flag, Library, Plus, SkipForward, type LucideIcon } from "lucide-react";
import { createTaskTemplateAction, postponeTaskAction, skipTaskAction } from "@/app/actions";
import { PlanEditor, type WeeklyPlanRuleInput } from "@/components/PlanEditor";
import { Shell } from "@/components/Shell";
import { loadPlanData } from "@/lib/dashboard-data";
import { requireParentPageContext } from "@/lib/parent-page";
import { renderParentPageFallback } from "@/lib/parent-page-render";

type PageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

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

const categoryOptions = [
  { value: "study", label: "学习" },
  { value: "reading", label: "阅读" },
  { value: "exercise", label: "运动" },
  { value: "habit", label: "习惯" },
  { value: "custom", label: "自定义" }
];

export default async function PlanPage({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const ctx = await requireParentPageContext();
  const fallback = renderParentPageFallback(ctx);
  if (fallback) return fallback;

  const data = ctx.data;
  if (!data) return null;
  const { weeklyPlan, todayTasks, taskTemplates } = loadPlanData(data);
  const rules = weeklyPlan.rules.map<WeeklyPlanRuleInput>((rule, index) => ({
    title: rule.title,
    category: rule.category,
    submissionType: rule.submissionType,
    weekdays: rule.weekdays,
    isCore: rule.isCore,
    requireReview: rule.requireReview,
    sortOrder: index
  }));
  const coreRules = weeklyPlan.rules.filter((rule) => rule.isCore);
  const weeklyRhythm = [1, 2, 3, 4, 5, 6, 7].map((day) => {
    const dayRules = weeklyPlan.rules.filter((rule) => rule.weekdays.includes(day));
    return {
      day,
      total: dayRules.length,
      core: dayRules.filter((rule) => rule.isCore).length,
      titles: dayRules.map((rule) => rule.title)
    };
  });
  const pendingToday = todayTasks.filter((task) => !["approved", "skipped", "postponed"].includes(task.status)).length;

  return (
    <Shell>
      <header className="topbar">
        <div>
          <p className="muted">安排本周节奏，保存后会生成对应的今日任务</p>
          <h1 className="page-title">计划</h1>
        </div>
      </header>

      <section className="dashboard-grid" aria-label="本周计划">
        {singleParam(params.actionError) ? <div className="form-error span-12"><span>{singleParam(params.actionError)}</span><a className="secondary-button" href="/plan">重新加载</a></div> : null}
        {singleParam(params.actionSuccess) ? <p className="form-success span-12">{singleParam(params.actionSuccess)}</p> : null}
        <article className="panel span-12 plan-command">
          <div>
            <p className="muted">本周计划驾驶台</p>
            <h2>{weeklyPlan.weekStartDate} 至 {weeklyPlan.weekEndDate}</h2>
            <p className="muted">先确认今日需要处理的任务，再检查本周核心任务分布是否均衡。</p>
          </div>
          <div className="plan-command-stats">
            <PlanStat icon={CalendarDays} label="今日待处理" value={pendingToday} />
            <PlanStat icon={BarChart3} label="计划规则" value={weeklyPlan.rules.length} />
            <PlanStat icon={Flag} label="核心任务" value={coreRules.length} />
          </div>
          <div className="week-rhythm" aria-label="本周任务节奏">
            {weeklyRhythm.map((item) => (
              <div className="week-rhythm-day" key={item.day}>
                <span>周{weekdayLabel(item.day)}</span>
                {item.total === 0 ? <small>本周暂无安排</small> : <><strong>{item.total}</strong><small>{item.core} 个核心任务</small><div className="week-rhythm-titles">{item.titles.map((title) => <span key={title}>{title}</span>)}</div></>}
              </div>
            ))}
          </div>
        </article>

        <article className="panel span-7">
          <h2>今日任务</h2>
          <div className="task-list">
            {todayTasks.length === 0 ? (
              <div className="empty-state">
                <CalendarDays size={20} aria-hidden="true" />
                <div>
                  <strong>今天没有已生成任务</strong>
                  <p className="muted">保存本周计划后，系统会按计划生成当天任务。</p>
                </div>
              </div>
            ) : (
              todayTasks.map((task) => {
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
                      {data.source === "api" && open ? (
                        <>
                          <form action={skipTaskAction}>
                            <input name="returnTo" type="hidden" value="/plan" />
                            <input name="taskId" type="hidden" value={task.id} />
                            <input name="reason" type="hidden" value="家庭临时调整" />
                            <button className="icon-button" aria-label={`${task.title} 跳过`} type="submit">
                              <SkipForward size={16} aria-hidden="true" />
                            </button>
                          </form>
                          <form action={postponeTaskAction}>
                            <input name="returnTo" type="hidden" value="/plan" />
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

        <article className="panel span-5">
          <h2>任务模板库</h2>
          <div className="template-list">
            {taskTemplates.length === 0 ? (
              <div className="empty-state">
                <Library size={20} aria-hidden="true" />
                <div>
                  <strong>还没有任务模板</strong>
                  <p className="muted">创建模板后，可以在计划规则编辑器中加入本周。</p>
                </div>
              </div>
            ) : (
              taskTemplates.map((template) => (
                <div className="template-row template-card-row" key={template.id}>
                  <span className="module-card-icon">
                    <Library size={18} aria-hidden="true" />
                  </span>
                  <div>
                    <strong>{template.title}</strong>
                    <p className="muted" style={{ margin: "6px 0 0" }}>
                      {categoryLabel(template.category)} · {submissionLabel(template.submissionType)}
                    </p>
                  </div>
                </div>
              ))
            )}
          </div>
          <h3 className="section-subtitle">创建模板</h3>
          <p className="muted">模板暂不支持编辑/归档（后端待支持）。</p>
          {data.source === "api" ? (
            <form action={createTaskTemplateAction} className="plan-editor-form">
              <input name="returnTo" type="hidden" value="/plan" />
              <label>
                标题
                <input name="title" placeholder="每日朗读" />
              </label>
              <label>
                分类
                <select name="category" defaultValue="reading">
                  {categoryOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                提交类型
                <select name="submissionType" defaultValue="photo">
                  <option value="photo">photo</option>
                  <option value="audio">audio</option>
                  <option value="video">video</option>
                  <option value="manual">manual</option>
                </select>
              </label>
              <button className="primary-button" type="submit">
                <Plus size={16} aria-hidden="true" />
                创建模板
              </button>
            </form>
          ) : (
            <div className="empty-state section-subtitle">
              <Library size={20} aria-hidden="true" />
              <div>
                {data.coreApiError ? (
                  <>
                    <strong>数据加载失败: {data.coreApiError}</strong>
                    <p className="muted">请稍后重试或检查服务状态。</p>
                  </>
                ) : (
                  <>
                    <strong>样例数据暂不支持创建</strong>
                    <p className="muted">连接 Core API 后可创建任务模板。</p>
                  </>
                )}
              </div>
            </div>
          )}
        </article>

        <article className="panel span-7">
          <h2>本周计划规则</h2>
          <div className="task-list">
            {weeklyPlan.rules.length === 0 ? (
              <div className="empty-state">
                <CalendarDays size={20} aria-hidden="true" />
                <div>
                  <strong>还没有本周计划</strong>
                  <p className="muted">先创建任务模板并保存周计划，今日任务会随之生成。</p>
                </div>
              </div>
            ) : (
              weeklyPlan.rules.map((rule) => (
                <div className="task-row" key={rule.title}>
                  <div>
                    <strong>{rule.title}</strong>
                    <p className="muted" style={{ margin: "6px 0 0" }}>
                      {categoryLabel(rule.category)} · {submissionLabel(rule.submissionType)}
                    </p>
                  </div>
                  <div className="task-actions">
                    <span className="status-pill status-blue">{rule.weekdays.length} 天</span>
                    {rule.isCore ? <span className="status-pill status-ready">核心</span> : null}
                  </div>
                </div>
              ))
            )}
          </div>
        </article>

        <article className="panel span-12">
          <PlanEditor
            childId={weeklyPlan.childId}
            disabled={data.source !== "api"}
            endDate={weeklyPlan.weekEndDate}
            rewardMode={weeklyPlan.rewardMode}
            rules={rules}
            startDate={weeklyPlan.weekStartDate}
            templates={taskTemplates}
            weekId={weeklyPlan.weekId}
          />
        </article>
      </section>
    </Shell>
  );
}

function PlanStat({ icon: Icon, label, value }: Readonly<{ icon: LucideIcon; label: string; value: number }>) {
  return (
    <div className="plan-stat">
      <Icon size={18} aria-hidden="true" />
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  );
}

function weekdayLabel(day: number) {
  return ["一", "二", "三", "四", "五", "六", "日"][day - 1] ?? String(day);
}

function categoryLabel(value: string) {
  return categoryOptions.find((option) => option.value === value)?.label ?? value;
}

function submissionLabel(value: string) {
  return { photo: "照片", audio: "音频", video: "视频", manual: "手动" }[value as "photo" | "audio" | "video" | "manual"] ?? value;
}

function nextDate(date: string) {
  const parsed = new Date(`${date}T00:00:00Z`);
  if (Number.isNaN(parsed.getTime())) return date;
  parsed.setUTCDate(parsed.getUTCDate() + 1);
  return parsed.toISOString().slice(0, 10);
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
