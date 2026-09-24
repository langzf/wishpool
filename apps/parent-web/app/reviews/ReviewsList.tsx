"use client";

import { CheckCircle2, Image, LoaderCircle, Mic, Video } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { processReviewAction } from "@/app/actions";

type Review = { submissionId: string; taskTitle: string; category?: string; isCore?: boolean; submittedAt: string; aiSummary?: string | null; mediaType: string; thumbnailMedia?: { downloadUrl?: string | null; contentType: string } | null };
const categoryNames: Record<string, string> = { reading: "阅读", exercise: "运动", creativity: "创造", life: "生活", learning: "学习", habit: "习惯" };

export function ReviewsList({ reviews, enabled }: { reviews: Review[]; enabled: boolean }) {
  const [selected, setSelected] = useState<string[]>([]); const [feedback, setFeedback] = useState(""); const [working, setWorking] = useState(false); const [loading, setLoading] = useState(false); const [message, setMessage] = useState<{ kind: "success" | "error"; text: string } | null>(null); const [progress, setProgress] = useState<{ done: number; total: number } | null>(null);
  useEffect(() => setLoading(false), []);
  const ids = useMemo(() => reviews.map((review) => review.submissionId), [reviews]); const allSelected = ids.length > 0 && ids.every((id) => selected.includes(id));
  async function process(idsToProcess: string[], decision: "approved" | "needs_revision") {
    if (!idsToProcess.length || working || !enabled) return; setWorking(true); setMessage(null); setProgress({ done: 0, total: idsToProcess.length }); let success = 0; const failures: string[] = [];
    for (let index = 0; index < idsToProcess.length; index += 1) { const form = new FormData(); form.set("submissionId", idsToProcess[index]); form.set("decision", decision); form.set("feedbackText", feedback); const result = await processReviewAction(form); if (result.ok) success += 1; else failures.push(`${findTitle(idsToProcess[index])}：${result.message}`); setProgress({ done: index + 1, total: idsToProcess.length }); }
    setWorking(false); setProgress(null); setSelected([]); setMessage(failures.length ? { kind: "error", text: `成功 ${success} 条 / 失败 ${failures.length} 条。${failures.join("；")}` } : { kind: "success", text: `成功处理 ${success} 条，已清空选择。` });
  }
  function findTitle(id: string) { return reviews.find((review) => review.submissionId === id)?.taskTitle ?? id; }
  if (loading) return <div className="review-list" aria-busy="true"><div className="review-skeleton" /><div className="review-skeleton" /></div>;
  if (!reviews.length) return <div className="empty-state"><CheckCircle2 size={20} aria-hidden="true" /><div><strong>暂无待审核任务</strong><p className="muted">孩子提交任务后会出现在这里。</p></div></div>;
  return <div>
    {message ? <p className={message.kind === "success" ? "form-success" : "form-error"} role="status">{message.text}</p> : null}
    <div className="review-bulk-toolbar"><label className="inline-check"><input type="checkbox" checked={allSelected} onChange={() => setSelected(allSelected ? [] : ids)} disabled={working} /> 全选</label><span className="muted">已选 {selected.length} 条</span><textarea aria-label="批量审核反馈" value={feedback} onChange={(event) => setFeedback(event.target.value)} placeholder="输入反馈，将应用到所有选中项" rows={2} disabled={working} /><button className="primary-button" type="button" disabled={!selected.length || working} onClick={() => process(selected, "approved")}>{working ? "处理中" : "批量通过"}</button><button className="secondary-button" type="button" disabled={!selected.length || working} onClick={() => process(selected, "needs_revision")}>{working ? "处理中" : "批量退回"}</button></div>
    {progress ? <p className="inline-hint" role="status">处理中 {progress.done}/{progress.total}</p> : null}
    <div className="review-list">{reviews.map((review) => <ReviewRow key={review.submissionId} review={review} selected={selected.includes(review.submissionId)} disabled={working || !enabled} feedback={feedback} onToggle={() => setSelected((current) => current.includes(review.submissionId) ? current.filter((item) => item !== review.submissionId) : [...current, review.submissionId])} onProcess={(decision) => process([review.submissionId], decision)} />)}</div>
  </div>;
}

function ReviewRow({ review, selected, disabled, feedback, onToggle, onProcess }: { review: Review; selected: boolean; disabled: boolean; feedback: string; onToggle: () => void; onProcess: (decision: "approved" | "needs_revision") => void }) {
  const category = categoryNames[review.category ?? ""] ?? (review.category || "其他");
  return <div className="review-row review-row-enhanced"><input type="checkbox" checked={selected} onChange={onToggle} disabled={disabled} aria-label={`选择${review.taskTitle}`} /><div className="review-thumb">{review.thumbnailMedia?.downloadUrl ? <img src={review.thumbnailMedia.downloadUrl} alt={`${review.taskTitle} 缩略图`} /> : <><MediaIcon type={review.mediaType} /><span>无媒体</span></>}</div><div className="review-content"><div className="review-heading"><a href={`/reviews/${encodeURIComponent(review.submissionId)}`}><strong>{review.taskTitle}</strong></a>{review.isCore ? <span className="status-pill status-ready">核心任务</span> : null}</div><p className="muted review-meta">{category} · 提交于 {formatDate(review.submittedAt)}</p><p className="review-summary">{review.aiSummary || "暂无 AI 预审"}</p></div><div className="review-actions"><a className="secondary-button" href={`/reviews/${encodeURIComponent(review.submissionId)}`}>查看详情</a><button className="primary-button" type="button" disabled={disabled} onClick={() => onProcess("approved")}>{disabled ? <LoaderCircle className="wish-spin" size={16} /> : null}通过</button><button className="secondary-button" type="button" disabled={disabled} onClick={() => onProcess("needs_revision")}>退回</button>{feedback ? <span className="muted">将使用已填反馈</span> : null}</div></div>;
}
function formatDate(value: string) { const date = new Date(value); return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(date); }
function MediaIcon({ type }: { type: string }) { if (type === "audio") return <Mic size={24} aria-hidden="true" />; if (type === "video") return <Video size={24} aria-hidden="true" />; return <Image size={24} aria-hidden="true" />; }
