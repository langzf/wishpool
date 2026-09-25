import { Shell } from "@/components/Shell";
import { loadReviewsData } from "@/lib/dashboard-data";
import { requireParentPageContext } from "@/lib/parent-page";
import { renderParentPageFallback } from "@/lib/parent-page-render";
import { ReviewsList } from "./ReviewsList";

type PageProps = { searchParams?: Promise<Record<string, string | string[] | undefined>> };

export default async function ReviewsPage({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const ctx = await requireParentPageContext();
  const fallback = renderParentPageFallback(ctx);
  if (fallback) return fallback;
  if (!ctx.data) return null;
  const { pendingReviews } = loadReviewsData(ctx.data);
  const param = (value: string | string[] | undefined) => Array.isArray(value) ? value[0] : value;
  return <Shell>
    <header className="topbar"><div><p className="muted">AI 预审后的提交会集中在这里</p><h1 className="page-title">待审核</h1></div></header>
    {param(params.actionError) ? <p aria-live="assertive" className="form-error" role="alert">{param(params.actionError)}</p> : null}
    {param(params.actionSuccess) ? <p aria-live="polite" className="form-success" role="status">{param(params.actionSuccess)}</p> : null}
    <section className="dashboard-grid" aria-label="待审核列表"><article className="panel span-12"><ReviewsList reviews={pendingReviews} enabled={ctx.data.source === "api"} /></article></section>
  </Shell>;
}
