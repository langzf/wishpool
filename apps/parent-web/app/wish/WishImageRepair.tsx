"use client";

import { Check, ImagePlus, Loader2, Sparkles } from "lucide-react";
import { useState } from "react";
import { attachWishImage, createWishImageGeneration, getWishImageGenerationJob } from "@/app/actions";

type Candidate = { media: { id: string; downloadUrl?: string | null }; sourceWishTitle?: string | null; score: number };

export function WishImageRepair({ familyId, childId, wishId, title, note }: { familyId: string; childId: string; wishId: string; title: string; note: string }) {
  const [items, setItems] = useState<Candidate[]>([]);
  const [selected, setSelected] = useState("");
  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [message, setMessage] = useState("");

  async function load() {
    setLoading(true); setMessage("");
    try {
      const response = await fetch("/wish/image-candidates", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ familyId, childId, title, note, limit: 12 }) });
      if (!response.ok) throw new Error("图片列表加载失败，请稍后重试。");
      setItems(((await response.json()) as { items?: Candidate[] }).items ?? []);
    } catch (error) { setMessage(error instanceof Error ? error.message : "图片列表加载失败。"); }
    finally { setLoading(false); }
  }

  async function generate() {
    setGenerating(true); setMessage("AI 正在生成图片，请稍候…");
    try {
      let job = await createWishImageGeneration({ familyId, childId, wishId, title, note });
      for (let attempt = 0; attempt < 30 && job.status !== "succeeded"; attempt += 1) { await new Promise((resolve) => window.setTimeout(resolve, 2000)); job = await getWishImageGenerationJob(job.id); }
      if (job.status !== "succeeded" || !job.mediaAssetId) throw new Error(job.errorMessage || "图片生成失败，请重试。");
      setMessage("图片已生成并关联到当前心愿。");
    } catch (error) { setMessage(error instanceof Error ? error.message : "图片生成失败，请重试。"); }
    finally { setGenerating(false); }
  }

  async function choose(id: string) {
    if (selected === id) { setSelected(""); setMessage("已取消选择。"); return; }
    setSelected(id); setMessage("正在关联图片…");
    try { await attachWishImage(wishId, id, "reused"); setMessage("图片已关联到当前心愿。"); }
    catch (error) { setMessage(error instanceof Error ? error.message : "图片关联失败，请重试。"); }
  }

  return <div className="wish-image-repair">
    <div className="wish-image-repair-copy"><ImagePlus size={20} aria-hidden="true" /><div><strong>当前心愿还没有配图</strong><p className="muted">选择一张家庭图片，或重新生成一张，让心愿卡完整呈现。</p></div></div>
    <div className="wish-repair-actions"><button className="secondary-button" disabled={loading} onClick={load} type="button">{loading ? <Loader2 className="wish-spin" size={16} /> : <ImagePlus size={16} />}{loading ? "加载中…" : "选择已有图片"}</button><button className="secondary-button" disabled={generating} onClick={generate} type="button">{generating ? <Loader2 className="wish-spin" size={16} /> : <Sparkles size={16} />}{generating ? "生成中…" : "重新生成图片"}</button></div>
    {items.length ? <div className="wish-repair-grid">{items.map((item) => <button aria-pressed={selected === item.media.id} className={selected === item.media.id ? "wish-repair-item selected" : "wish-repair-item"} key={item.media.id} onClick={() => choose(item.media.id)} type="button">{item.media.downloadUrl ? <img alt="可选心愿图片" src={item.media.downloadUrl} /> : <span className="wish-image-empty"><ImagePlus size={22} /></span>}<span>{selected === item.media.id ? <Check size={15} /> : null}{item.sourceWishTitle || "家庭图片"}</span></button>)}</div> : null}
    {message ? <p aria-live="polite" className="wish-image-message">{message}</p> : null}
  </div>;
}
