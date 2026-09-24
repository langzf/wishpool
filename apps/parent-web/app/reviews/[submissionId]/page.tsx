import Link from "next/link";
import { ArrowLeft, FileAudio, FileVideo, Image as ImageIcon } from "lucide-react";
import { ReviewDecisionForm } from "@/app/reviews/ReviewDecisionForm";
import { Shell } from "@/components/Shell";
import { coreGetJson } from "@/lib/core-client";
import { requireParentPageContext } from "@/lib/parent-page";
import { renderParentPageFallback } from "@/lib/parent-page-render";

type PageProps = {
  params: Promise<{ submissionId: string }>;
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

type ReviewDetail = {
  id: string;
  submittedAt: string;
  status: string;
  submissionType: string;
  task: {
    title: string;
    category: string;
    scheduledDate: string;
    isCore: boolean;
  } | null;
  media: Array<{ id: string; contentType: string; downloadUrl?: string | null }>;
  aiPrecheck: Record<string, unknown> | null;
  review: Record<string, unknown> | null;
};

export default async function ReviewDetailPage({ params, searchParams }: PageProps) {
  const { submissionId } = await params;
  const query = (await searchParams) ?? {};
  const ctx = await requireParentPageContext();
  const fallback = renderParentPageFallback(ctx);
  if (fallback) return fallback;

  let detail: ReviewDetail;
  let loadError: string | null = null;
  try {
    detail = await coreGetJson<ReviewDetail>(`/reviews/${encodeURIComponent(submissionId)}/detail`, ctx.session?.accessToken);
  } catch (error) {
    detail = {
      id: submissionId,
      submittedAt: "",
      status: "",
      submissionType: "",
      task: null,
      media: [],
      aiPrecheck: null,
      review: null
    };
    loadError = error instanceof Error ? error.message : "详情加载失败，请稍后重试。";
  }

  const returnTo = `/reviews/${encodeURIComponent(submissionId)}`;
  const actionError = singleParam(query.actionError);
  const feedbackText = singleParam(query.feedbackText) ?? "";

  return (
    <Shell>
      <header className="topbar">
        <div>
          <Link href="/reviews" className="muted"><ArrowLeft size={16} aria-hidden="true" /> 返回待审核</Link>
          <h1 className="page-title">审核详情</h1>
          <p className="muted">查看原始提交内容后，再决定是否通过。</p>
        </div>
      </header>
      {loadError ? <p className="form-error">详情加载失败：{loadError}</p> : null}
      {actionError ? <p className="form-error">{actionError}</p> : null}
      {!loadError ? (
        <section className="dashboard-grid">
          <article className="panel span-7">
            <h2>原始材料</h2>
            {detail.media.length === 0 ? (
              <div className="empty-state"><span>暂无媒体材料</span><p className="muted">这条提交没有可预览的图片、音频或视频。</p></div>
            ) : (
              <div style={{ display: "grid", gap: 16 }}>
                {detail.media.map((media) => <MediaPreview key={media.id} media={media} />)}
              </div>
            )}
          </article>
          <article className="panel span-5">
            <h2>任务信息</h2>
            {detail.task ? <dl style={{ display: "grid", gap: 10 }}>
              <Info label="标题" value={detail.task.title} />
              <Info label="类别" value={detail.task.category} />
              <Info label="计划日期" value={detail.task.scheduledDate} />
              <Info label="核心任务" value={detail.task.isCore ? "是" : "否"} />
            </dl> : <p className="muted">暂无任务信息。</p>}
            <h2 style={{ marginTop: 24 }}>AI 预审</h2>
            {detail.aiPrecheck ? <pre style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}>{JSON.stringify(detail.aiPrecheck, null, 2)}</pre> : <p className="muted">暂无 AI 预审</p>}
          </article>
          {ctx.data?.source === "api" ? <div className="span-12"><ReviewDecisionForm submissionId={submissionId} returnTo={returnTo} feedbackText={feedbackText} /></div> : null}
        </section>
      ) : null}
    </Shell>
  );
}

function MediaPreview({ media }: { media: ReviewDetail["media"][number] }) {
  if (!media.downloadUrl) return <p className="muted">该媒体暂时没有可用的预览地址。</p>;
  if (media.contentType.startsWith("image/")) return <div><ImageIcon size={18} aria-hidden="true" /><img src={media.downloadUrl} alt="提交的图片" style={{ display: "block", maxWidth: "100%", marginTop: 8, borderRadius: 8 }} /></div>;
  if (media.contentType.startsWith("audio/")) return <div><FileAudio size={18} aria-hidden="true" /><audio controls src={media.downloadUrl} style={{ width: "100%", marginTop: 8 }}>您的浏览器不支持音频播放。</audio></div>;
  if (media.contentType.startsWith("video/")) return <div><FileVideo size={18} aria-hidden="true" /><video controls src={media.downloadUrl} style={{ display: "block", maxWidth: "100%", marginTop: 8 }}>您的浏览器不支持视频播放。</video></div>;
  return <p className="muted">暂不支持预览此类型的媒体（{media.contentType}）。</p>;
}

function Info({ label, value }: { label: string; value: string }) {
  return <div><dt className="muted">{label}</dt><dd style={{ margin: 0 }}>{value}</dd></div>;
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
