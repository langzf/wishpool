"use client";

import { Check, ImagePlus, Loader2, RefreshCw, Sparkles, Upload, X } from "lucide-react";
import { useEffect, useMemo, useRef, useState, useTransition, type ChangeEvent } from "react";
import { createWishAction, createWishImageGeneration, createWishUploadSession, finalizeWishUpload, getWishImageGenerationJob } from "@/app/actions";

type Candidate = { media: { id: string; contentType?: string; downloadUrl?: string | null }; score: number; reason: string; sourceWishTitle?: string | null };
type Provider = { code: string; displayName: string; modelName: string; isDefault: boolean };
type Props = { familyId: string; childId: string; weekId: string; rewardMode: string; onDirtyChange?: (dirty: boolean) => void };

const modes = [
  { value: "grid_reveal", label: "网格揭晓", detail: "整齐分块，逐块点亮" },
  { value: "puzzle_lines", label: "拼图线条", detail: "拼图边缘，逐步揭示" },
  { value: "irregular", label: "多棱碎片", detail: "水晶切面，不规则揭示" }
];

export function WishCreateForm({ familyId, childId, weekId, rewardMode, onDirtyChange }: Props) {
  const [title, setTitle] = useState("");
  const [note, setNote] = useState("");
  const [fragments, setFragments] = useState(10);
  const [mode, setMode] = useState("grid_reveal");
  const [providers, setProviders] = useState<Provider[]>([]);
  const [providerCode, setProviderCode] = useState("");
  const [candidates, setCandidates] = useState<Candidate[]>([]);
  const [libraryOpen, setLibraryOpen] = useState(false);
  const [selectedId, setSelectedId] = useState("");
  const [previewUrl, setPreviewUrl] = useState("");
  const [skipImage, setSkipImage] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState("");
  const [generating, setGenerating] = useState(false);
  const [generationMessage, setGenerationMessage] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPending, startTransition] = useTransition();
  const fileRef = useRef<HTMLInputElement>(null);
  const uploadPreviewRef = useRef("");

  useEffect(() => {
    onDirtyChange?.(
      title.trim().length > 0 ||
      note.trim().length > 0 ||
      fragments !== 10 ||
      mode !== "grid_reveal" ||
      Boolean(selectedId || previewUrl || skipImage || uploading || generating || isSubmitting)
    );
  }, [fragments, generating, isSubmitting, mode, note, onDirtyChange, previewUrl, selectedId, skipImage, title, uploading]);

  useEffect(() => {
    let cancelled = false;
    fetch("/wish/image-model-providers", { cache: "no-store" }).then((response) => response.ok ? response.json() : null).then((body: { providers?: Provider[]; selectedProviderCode?: string | null } | null) => {
      if (cancelled || !body) return;
      const next = body.providers ?? [];
      setProviders(next);
      setProviderCode(body.selectedProviderCode ?? next.find((item) => item.isDefault)?.code ?? next[0]?.code ?? "");
    }).catch(() => undefined);
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (title.trim().length < 2 || skipImage) { setCandidates([]); return; }
    const timer = window.setTimeout(() => startTransition(async () => {
      try {
        const response = await fetch("/wish/image-candidates", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ familyId, childId, title: title.trim(), note, limit: 9 }) });
        setCandidates(response.ok ? ((await response.json()) as { items?: Candidate[] }).items ?? [] : []);
      } catch { setCandidates([]); }
    }), 420);
    return () => window.clearTimeout(timer);
  }, [childId, familyId, note, skipImage, title]);

  const visibleCandidates = useMemo(() => candidates.slice(0, 6), [candidates]);

  function chooseImage(id: string, url: string) {
    setSelectedId(selectedId === id ? "" : id);
    setPreviewUrl(selectedId === id ? "" : url);
    setSkipImage(false);
    setGenerationMessage("");
  }

  async function uploadFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith("image/")) { setUploadError("请选择图片文件。"); return; }
    setUploadError(""); setUploading(true); setSkipImage(false); setSelectedId("");
    const localUrl = URL.createObjectURL(file);
    uploadPreviewRef.current = localUrl; setPreviewUrl(localUrl);
    try {
      const session = await createWishUploadSession(familyId, childId, { contentType: file.type, sizeBytes: file.size });
      const response = await fetch(session.uploadUrl, { method: "PUT", headers: { "Content-Type": file.type }, body: file });
      if (!response.ok) throw new Error("图片上传失败，请重试。");
      const result = await finalizeWishUpload(session.mediaId);
      setSelectedId(result.mediaId); setGenerationMessage("图片已上传并选中");
    } catch (error) { setUploadError(error instanceof Error ? error.message : "图片上传失败，请重试。"); }
    finally { setUploading(false); }
  }

  async function generateImage() {
    if (!title.trim()) { setGenerationMessage("请先填写心愿标题。"); return; }
    setGenerating(true); setGenerationMessage("AI 正在生成图片，预计需要一点时间…"); setUploadError("");
    try {
      let job = await createWishImageGeneration({ familyId, childId, title: title.trim(), note, providerCode });
      for (let attempt = 0; attempt < 30; attempt += 1) {
        if (job.status === "succeeded" && job.mediaAssetId) {
          setSelectedId(job.media?.id ?? job.mediaAssetId); setPreviewUrl(job.media?.downloadUrl ?? ""); setSkipImage(false); setGenerationMessage("AI 图片已生成并选中"); return;
        }
        if (job.status === "failed_final" || job.status === "cancelled") throw new Error(job.errorMessage || "图片生成失败，请重试。");
        await new Promise((resolve) => window.setTimeout(resolve, 2000));
        job = await getWishImageGenerationJob(job.id);
      }
      throw new Error("图片仍在生成中，请稍后重试。");
    } catch (error) { setGenerationMessage(error instanceof Error ? error.message : "图片生成失败，请重试。"); }
    finally { setGenerating(false); }
  }

  function clearImage() {
    setSelectedId(""); setPreviewUrl(""); setSkipImage(false); setGenerationMessage(""); setUploadError("");
    if (uploadPreviewRef.current) { URL.revokeObjectURL(uploadPreviewRef.current); uploadPreviewRef.current = ""; }
    if (fileRef.current) fileRef.current.value = "";
  }

  async function submit(formData: FormData) {
    if (title.trim().length < 2) { setSubmitError("请填写至少 2 个字的心愿标题。"); return; }
    if (!Number.isInteger(fragments) || fragments < 1 || fragments > 100) { setSubmitError("目标碎片数需为 1–100 的整数。"); return; }
    setIsSubmitting(true); setSubmitError("");
    try {
      formData.set("imageMediaId", skipImage ? "" : selectedId);
      await createWishAction(formData);
    } catch (error) { setSubmitError(error instanceof Error ? error.message : "保存失败，请重试。"); setIsSubmitting(false); }
  }

  return (
    <form action={submit} className="wish-form wish-create-card">
      <input name="returnTo" type="hidden" value="/wish" /><input name="childId" type="hidden" value={childId} /><input name="weekId" type="hidden" value={weekId} /><input name="rewardMode" type="hidden" value={rewardMode} /><input name="imageMediaId" type="hidden" value={skipImage ? "" : selectedId} /><input name="providerCode" type="hidden" value={providerCode} /><input name="fragmentVisualMode" type="hidden" value={mode} />
      <label>心愿标题 <span className="wish-required">必填</span><input autoFocus maxLength={40} name="title" onChange={(event) => { setTitle(event.target.value); setSubmitError(""); }} placeholder="例如：周末去自然博物馆" required value={title} /><small className="wish-field-count">{title.length}/40</small></label>
      <label>备注 <span className="wish-optional">选填</span><textarea maxLength={120} name="note" onChange={(event) => setNote(event.target.value)} placeholder="完成本周计划后，一起去实现它" value={note} /><small className="wish-field-count">{note.length}/120</small></label>
      <label>目标碎片数 <span className="wish-optional">决定完成节奏</span><input max="100" min="1" name="requiredFragments" onChange={(event) => setFragments(Number(event.target.value))} type="number" value={fragments} /></label>
      <fieldset className="wish-mode-fieldset"><legend>碎片呈现方式</legend><div className="wish-mode-picker" role="radiogroup" aria-label="碎片呈现方式">{modes.map((item) => <button aria-checked={mode === item.value} className={mode === item.value ? "wish-mode-option selected" : "wish-mode-option"} key={item.value} onClick={() => setMode(item.value)} role="radio" type="button"><span>{item.label}</span><small>{item.detail}</small>{mode === item.value ? <Check size={15} aria-hidden="true" /> : null}</button>)}</div></fieldset>
      {providers.length > 0 ? <label>AI 生成模型<select onChange={(event) => setProviderCode(event.target.value)} value={providerCode}>{providers.map((item) => <option key={item.code} value={item.code}>{item.displayName} · {item.modelName}</option>)}</select></label> : null}
      <section aria-label="心愿图片" className="wish-image-picker"><div className="wish-image-picker-head"><div><strong>心愿图片</strong><small>让这份期待更具象</small></div><span>{uploading ? "上传中…" : isPending ? "查找中…" : selectedId ? "已选择" : skipImage ? "暂不使用" : "可选"}</span></div>
        {previewUrl && !skipImage ? <div className="wish-image-preview"><img alt="已选择的心愿图片" onError={() => setPreviewUrl("")} src={previewUrl} /><div><strong>已选择图片</strong><button className="text-button" onClick={clearImage} type="button"><X size={14} />取消选择</button></div></div> : null}
        {libraryOpen && !skipImage ? <div className="wish-candidate-grid">{visibleCandidates.map((item) => <button aria-pressed={selectedId === item.media.id} className={selectedId === item.media.id ? "wish-candidate-card selected" : "wish-candidate-card"} key={item.media.id} onClick={() => chooseImage(item.media.id, item.media.downloadUrl ?? "")} type="button">{item.media.downloadUrl ? <img alt="家庭图片" src={item.media.downloadUrl} /> : <span className="wish-image-empty"><ImagePlus size={22} /></span>}<span>{selectedId === item.media.id ? <Check size={14} /> : null}{item.sourceWishTitle || "家庭图片"}</span><small>{item.score}% · {item.reason}</small></button>)}</div> : null}
        {libraryOpen && visibleCandidates.length === 0 ? <div className="wish-image-empty"><ImagePlus size={20} /><span>{title.trim().length < 2 ? "先填写标题，我们会为你找相似的家庭图片" : "暂时没有找到可复用的家庭图片"}</span></div> : null}
        {uploadError ? <p className="wish-upload-status error">{uploadError}</p> : null}{generationMessage ? <p className={generating ? "wish-upload-status" : "wish-upload-status"}>{generating ? <Loader2 className="wish-spin" size={18} /> : <Sparkles size={18} />}{generationMessage}</p> : null}
        <div className="wish-image-actions"><button className="secondary-button" onClick={() => setLibraryOpen((value) => !value)} type="button"><ImagePlus size={16} />{libraryOpen ? "收起图片" : "选择已有图片"}</button><button className="secondary-button" disabled={!title.trim() || generating || uploading} onClick={generateImage} type="button"><Sparkles size={16} />{generating ? "生成中…" : "AI 生成"}</button><label className="secondary-button wish-upload-button"><Upload size={16} />上传图片<input accept="image/*" onChange={uploadFile} ref={fileRef} type="file" /></label><button className="secondary-button" onClick={() => { clearImage(); setSkipImage(true); }} type="button"><X size={16} />暂不使用</button></div>
      </section>
      {submitError ? <p aria-live="polite" className="form-error">{submitError}</p> : null}
      <button className="primary-button wish-submit-button" disabled={uploading || generating || isSubmitting} type="submit">{isSubmitting ? <Loader2 className="wish-spin" size={17} /> : <Sparkles size={17} />}{isSubmitting ? "保存中…" : "创建并激活"}</button>
    </form>
  );
}
