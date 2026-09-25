import { Bell } from "lucide-react";
import { markNotificationsReadAction, updateNotificationPreferenceAction } from "@/app/actions";
import { NotificationPreferenceSubmit } from "@/components/NotificationPreferenceSubmit";
import { Shell } from "@/components/Shell";
import { loadNotificationsData } from "@/lib/dashboard-data";
import { requireParentPageContext } from "@/lib/parent-page";
import { renderParentPageFallback } from "@/lib/parent-page-render";

type PageProps = { searchParams?: Promise<Record<string, string | string[] | undefined>> };

const labels: Record<string, string> = {
  child_submission_created: "孩子提交任务",
  ai_precheck_completed: "AI 预审完成",
  review_completed: "审核完成",
  wish_fragment_earned: "获得心愿碎片",
  wish_unlocked: "心愿已集满",
  wish_redeemed_memory_generated: "兑现纪念册生成",
  task_plan_changed: "计划有变动"
};

export default async function NotificationsPage({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const ctx = await requireParentPageContext();
  const fallback = renderParentPageFallback(ctx);
  if (fallback) return fallback;
  const data = ctx.data;
  if (!data) return null;
  const { notificationInbox, notificationPreferences } = loadNotificationsData(data);
  const unreadNotifications = notificationInbox.filter((notification) => notification.status !== "read");

  return (
    <Shell>
      <header className="topbar"><div><p className="muted">审核、任务变更和心愿动态会汇总在这里</p><h1 className="page-title">通知</h1></div></header>
      <section className="dashboard-grid" aria-label="通知">
        {singleParam(params.actionError) ? <p aria-live="assertive" className="form-error span-12" role="alert">{singleParam(params.actionError)}</p> : null}
        {singleParam(params.actionSuccess) ? <p aria-live="polite" className="form-success span-12" role="status">{singleParam(params.actionSuccess)}</p> : null}
        <article className="panel span-8">
          <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center", flexWrap: "wrap" }}><h2>通知收件箱</h2>
            {data.source === "api" && unreadNotifications.length > 0 ? <form action={markNotificationsReadAction}><input name="returnTo" type="hidden" value="/notifications" />{unreadNotifications.map((notification) => <input key={notification.id} name="notificationIds" type="hidden" value={notification.id} />)}<button className="secondary-button" type="submit"><Bell size={16} aria-hidden="true" />全部已读</button></form> : null}
          </div>
          <div className="notification-list">{notificationInbox.length === 0 ? <div className="empty-state"><Bell size={20} aria-hidden="true" /><div><strong>没有站内通知</strong><p className="muted">审核、任务变更、心愿解锁等提醒会汇总到这里。</p></div></div> : notificationInbox.map((notification) => <div className="notification-row" key={notification.id}><div><strong>{notification.title}</strong><p className="muted" style={{ margin: "6px 0 0" }}>{notification.body}</p><time className="notification-time" dateTime={notification.createdAt}>{formatNotificationTime(notification.createdAt)}</time></div><span className={`status-pill ${notification.status === "read" ? "status-ready" : "status-waiting"}`}>{notification.status === "read" ? "已读" : "未读"}</span></div>)}</div>
        </article>
        <article className="panel span-4">
          <h2>通知偏好</h2>
          <p className="muted">每类通知单独保存启用状态、站内信和推送渠道。没有记录的项目使用默认值，标记为“尚未保存”。</p>
          <div className="preference-list">{notificationPreferences.map((preference) => {
            const quietHours = preference.quietHours;
            return data.source === "api" ? <form action={updateNotificationPreferenceAction} className="preference-row" key={preference.notificationType} style={{ display: "block" }}>
              <input name="clearQuietHours" type="hidden" value="true" />
              <input name="returnTo" type="hidden" value="/notifications" /><input name="notificationType" type="hidden" value={preference.notificationType} />
              <strong>{labels[preference.notificationType] ?? "家庭提醒"}</strong><small className="preference-meta" style={{ display: "block", marginTop: 4 }}>{preference.saved ? "已从服务端加载" : "尚未保存"}</small>
              <div style={{ display: "flex", gap: 12, flexWrap: "wrap", marginTop: 10 }}><label><input defaultChecked={preference.enabled} name="enabled" type="checkbox" value="true" /> 启用</label><label><input defaultChecked={preference.channels.inbox} name="inbox" type="checkbox" value="true" /> 站内信</label><label><input defaultChecked={preference.channels.push} name="push" type="checkbox" value="true" /> 推送</label></div>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "end", marginTop: 10 }}><label>开始<input defaultValue={quietHours.start ?? ""} name="quietHoursStart" type="time" /></label><label>结束<input defaultValue={quietHours.end ?? ""} name="quietHoursEnd" type="time" /></label><input name="quietHoursTimezone" type="hidden" value={quietHours.timezone ?? "Asia/Shanghai"} /><span className="preference-meta">安静时段：{quietHours.start && quietHours.end ? `${quietHours.start}—${quietHours.end}` : "未设置"}</span></div>
              <div style={{ display: "flex", gap: 8, marginTop: 10 }}><NotificationPreferenceSubmit /><NotificationPreferenceSubmit clear /></div>
            </form> : <div className="preference-row" key={preference.notificationType}><strong>{labels[preference.notificationType] ?? "家庭提醒"}</strong><span className={`status-pill ${preference.enabled ? "status-ready" : "status-waiting"}`}>{preference.enabled ? "开启" : "关闭"}</span></div>;
          })}</div>
        </article>
      </section>
    </Shell>
  );
}

function formatNotificationTime(value: string) { const date = new Date(value); if (Number.isNaN(date.getTime())) return value; return new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(date); }
function singleParam(value: string | string[] | undefined): string | undefined { return Array.isArray(value) ? value[0] : value; }
