import Link from "next/link";
import { ArrowLeft, Clock3 } from "lucide-react";
import { exportMemoryAction } from "@/app/actions";
import { Shell } from "@/components/Shell";
import { coreGetJson } from "@/lib/core-client";
import { requireParentPageContext } from "@/lib/parent-page";
import { renderParentPageFallback } from "@/lib/parent-page-render";

type PageProps = {
  params: Promise<{ memoryId: string }>;
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

type MemoryDetail = {
  id?: string;
  title?: string | null;
  status?: string | null;
  summary?: {
    startDate?: string | null;
    endDate?: string | null;
    summary?: string | null;
    wishTitle?: string | null;
    approvedTaskCount?: number | null;
  } | null;
  items?: Array<{
    title?: string | null;
    category?: string | null;
    scheduledDate?: string | null;
  }> | null;
};

export default async function MemoryDetailPage({ params, searchParams }: PageProps) {
  const { memoryId } = await params;
  const query = (await searchParams) ?? {};
  const ctx = await requireParentPageContext();
  const fallback = renderParentPageFallback(ctx);
  if (fallback) return fallback;

  let detail: MemoryDetail | null = null;
  let loadError: string | null = null;
  try {
    detail = await coreGetJson<MemoryDetail>(`/memories/${encodeURIComponent(memoryId)}`, ctx.session?.accessToken);
  } catch (error) {
    loadError = error instanceof Error ? error.message : "纪念册详情加载失败，请稍后重试。";
  }

  const actionError = singleParam(query.actionError);
  const actionSuccess = singleParam(query.actionSuccess);
  const returnTo = `/memories/${encodeURIComponent(memoryId)}`;

  return (
    <Shell>
      <header className="topbar">
        <div>
          <Link href="/memories" className="muted"><ArrowLeft size={16} aria-hidden="true" /> 返回纪念册</Link>
          <h1 className="page-title">{detail?.title || "纪念册详情"}</h1>
          <p className="muted">查看本周成长记录和任务摘要。</p>
        </div>
      </header>

      {loadError ? <p className="form-error">详情加载失败：{loadError}</p> : null}
      {actionError ? <p className="form-error">{actionError}</p> : null}
      {actionSuccess ? <p className="form-success">{actionSuccess}</p> : null}

      {!loadError && detail ? (
        <section className="dashboard-grid">
          <article className="panel span-8">
            <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "flex-start" }}>
              <div>
                <h2>{detail.title || "暂无纪念册标题"}</h2>
                <p className="muted" style={{ margin: "8px 0 0" }}>
                  {formatStatus(detail.status)}
                </p>
              </div>
              {ctx.data?.source === "api" ? (
                <form action={exportMemoryAction}>
                  <input name="returnTo" type="hidden" value={returnTo} />
                  <input name="memoryId" type="hidden" value={memoryId} />
                  <input name="format" type="hidden" value="pdf" />
                  <button className="secondary-button" type="submit">导出</button>
                </form>
              ) : null}
            </div>

            <div style={{ display: "grid", gap: 12, marginTop: 20 }}>
              <Info label="周区间" value={formatDateRange(detail.summary?.startDate, detail.summary?.endDate)} />
              <Info label="本周总结" value={detail.summary?.summary || "暂无本周总结"} />
              <Info label="对应心愿" value={detail.summary?.wishTitle || "暂无对应心愿"} />
              <Info label="已确认任务" value={formatApprovedTaskCount(detail.summary?.approvedTaskCount)} />
            </div>
          </article>

          <article className="panel span-4">
            <h2>本周任务</h2>
            {detail.items && detail.items.length > 0 ? (
              <div className="memory-list">
                {detail.items.map((item, index) => (
                  <div className="memory-row" key={`${item.title ?? "item"}-${item.scheduledDate ?? "date"}-${index}`}>
                    <div>
                      <strong>{item.title || "暂无任务标题"}</strong>
                      <p className="muted" style={{ margin: "6px 0 0" }}>
                        {item.category || "暂无任务分类"}
                      </p>
                    </div>
                    <span className="muted">{item.scheduledDate || "暂无计划日期"}</span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="empty-state" style={{ marginTop: 16 }}>
                <Clock3 size={20} aria-hidden="true" />
                <div>
                  <strong>暂无任务记录</strong>
                  <p className="muted">本周还没有可展示的任务。</p>
                </div>
              </div>
            )}
          </article>
        </section>
      ) : null}
    </Shell>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="muted">{label}</dt>
      <dd style={{ margin: "4px 0 0" }}>{value}</dd>
    </div>
  );
}

function formatStatus(status?: string | null) {
  if (status === "generated") return "状态：已生成";
  if (status === "exported") return "状态：已导出";
  return status ? `状态：${status}` : "暂无状态";
}

function formatDateRange(startDate?: string | null, endDate?: string | null) {
  if (startDate && endDate) return `${startDate} 至 ${endDate}`;
  if (startDate) return startDate;
  if (endDate) return endDate;
  return "暂无周区间";
}

function formatApprovedTaskCount(count?: number | null) {
  return typeof count === "number" ? `${count} 个` : "暂无已确认任务数量";
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
