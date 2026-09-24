"use client";

import { useState } from "react";

type PrivacyType = "export" | "delete";
type PrivacyResponse = { id: string; status: string; requestType: PrivacyType };
const confirmationTexts = { export: "EXPORT FAMILY DATA", delete: "DELETE FAMILY DATA" } as const;
const statusLabels: Record<string, string> = { requested: "已提交", verifying: "正在核验", locking_family: "正在锁定家庭", deleting_records: "正在删除记录", deleting_objects: "正在删除文件", verifying_deletion: "正在核验删除结果", completed: "已完成", failed_needs_attention: "失败，需要人工处理" };

export function PrivacyActions({ familyId }: Readonly<{ familyId: string }>) {
  const [exportText, setExportText] = useState("");
  const [deleteText, setDeleteText] = useState("");
  const [reason, setReason] = useState("");
  const [pending, setPending] = useState<PrivacyType | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<PrivacyResponse | null>(null);

  async function submit(requestType: PrivacyType) {
    const confirmationText = requestType === "export" ? exportText.trim() : deleteText.trim();
    if (confirmationText !== confirmationTexts[requestType]) return;
    setPending(requestType); setError(null); setResult(null);
    try {
      const response = await fetch("/api/privacy", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ familyId, requestType, confirmationText, reason: reason.trim() || undefined }) });
      const body = (await response.json()) as PrivacyResponse & { error?: string };
      if (!response.ok) throw new Error(body.error || "隐私请求提交失败，请稍后重试。");
      setResult(body);
      if (requestType === "export") setExportText("");
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "隐私请求提交失败，请稍后重试。");
    } finally { setPending(null); }
  }

  const exportReady = exportText.trim() === confirmationTexts.export;
  const deleteReady = deleteText.trim() === confirmationTexts.delete;
  return (
    <div className="privacy-actions">
      <article className="privacy-card">
        <h2>导出家庭数据</h2>
        <p className="muted">我们会创建一份家庭数据导出包。提交后页面只展示 202 返回的当前状态，不会自动轮询。</p>
        <label className="privacy-label">请输入确认词：{confirmationTexts.export}<input value={exportText} onChange={(event) => setExportText(event.target.value)} placeholder={confirmationTexts.export} /></label>
        <button className="primary-button" type="button" disabled={!exportReady || pending !== null} onClick={() => void submit("export")}>{pending === "export" ? "提交中…" : "导出数据"}</button>
      </article>
      <article className="privacy-card privacy-danger">
        <h2>删除家庭</h2>
        <p>此操作将删除家庭的全部数据和文件，且不可恢复。建议先完成数据导出并妥善保存备份。</p>
        <p>删除请求提交后家庭会被锁定，后台将异步处理。请确认你已经理解风险。</p>
        <label className="privacy-label">请输入确认词：{confirmationTexts.delete}<input value={deleteText} onChange={(event) => setDeleteText(event.target.value)} placeholder={confirmationTexts.delete} /></label>
        <label className="privacy-label">删除原因（可选）<textarea value={reason} onChange={(event) => setReason(event.target.value)} rows={3} /></label>
        <button className="danger-button" type="button" disabled={!deleteReady || pending !== null} onClick={() => void submit("delete")}>{pending === "delete" ? "提交中…" : "删除家庭"}</button>
      </article>
      {error ? <p className="form-error privacy-message" role="alert">{error}</p> : null}
      {result ? <p className="form-success privacy-message" role="status">{result.requestType === "export" ? "导出请求" : "删除请求"}已提交，当前状态：{statusLabels[result.status] ?? result.status}。请求编号：{result.id}。后端暂无状态查询接口，页面不会轮询。</p> : null}
    </div>
  );
}
