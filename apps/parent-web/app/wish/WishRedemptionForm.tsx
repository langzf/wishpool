"use client";

import { useRef, useState } from "react";
import { Check, ImagePlus, Loader2, X } from "lucide-react";
import {
  createWishRedemptionUploadSession,
  finalizeWishRedemptionUpload,
  redeemWish
} from "@/app/actions";

type SelectedPhoto = { file: File; previewUrl: string };

export function WishRedemptionForm({ familyId, childId, wishId }: Readonly<{ familyId: string; childId: string; wishId: string }>) {
  const [open, setOpen] = useState(false);
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [parentNote, setParentNote] = useState("");
  const [childNote, setChildNote] = useState("");
  const [photos, setPhotos] = useState<SelectedPhoto[]>([]);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  function addPhotos(event: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? []);
    event.target.value = "";
    if (files.length === 0) return;
    if (photos.length + files.length > 3) {
      setError("最多上传 3 张照片，请减少后再选择。");
      return;
    }
    const invalid = files.find((file) => !file.type.startsWith("image/"));
    if (invalid) {
      setError("兑现照片只能上传图片文件。");
      return;
    }
    setError("");
    setPhotos((current) => [...current, ...files.map((file) => ({ file, previewUrl: URL.createObjectURL(file) }))]);
  }

  function removePhoto(index: number) {
    setPhotos((current) => {
      const removed = current[index];
      if (removed) URL.revokeObjectURL(removed.previewUrl);
      return current.filter((_, photoIndex) => photoIndex !== index);
    });
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (photos.length < 1 || photos.length > 3) {
      setError("请上传 1-3 张兑现照片。");
      return;
    }
    setPending(true);
    setError("");
    setMessage("正在上传照片，请稍候…");
    try {
      const mediaIds: string[] = [];
      for (const photo of photos) {
        const session = await createWishRedemptionUploadSession(familyId, childId, {
          contentType: photo.file.type,
          sizeBytes: photo.file.size
        });
        const uploadResponse = await fetch(session.uploadUrl, {
          method: "PUT",
          headers: { "Content-Type": photo.file.type },
          body: photo.file
        });
        if (!uploadResponse.ok) throw new Error(`照片上传失败（${uploadResponse.status}）。`);
        await finalizeWishRedemptionUpload(session.mediaId);
        mediaIds.push(session.mediaId);
      }
      setMessage("正在保存兑现记录…");
      await redeemWish({ wishId, redeemedDate: date, photoMediaIds: mediaIds, parentNote, childNote });
      setMessage("兑现记录已保存，照片也已留在心愿历史中。");
      setPhotos([]);
      setOpen(false);
      window.location.reload();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "兑现失败，请稍后重试。");
      setMessage("");
    } finally {
      setPending(false);
    }
  }

  if (!open) {
    return <button className="primary-button wish-redeem-trigger" onClick={() => setOpen(true)} type="button"><Check size={17} />核销 / 兑现</button>;
  }

  return (
    <section className="wish-redemption-card" aria-label="心愿兑现表单">
      <div className="wish-redemption-heading">
        <div><strong>记录这次兑现</strong><p className="muted">上传 1-3 张照片，留下这份期待实现的纪念。</p></div>
        <button aria-label="关闭兑现表单" className="icon-button" disabled={pending} onClick={() => setOpen(false)} type="button"><X size={18} /></button>
      </div>
      <form className="wish-form" onSubmit={submit}>
        <label>兑现日期<input disabled={pending} onChange={(event) => setDate(event.target.value)} required type="date" value={date} /></label>
        <label>家长留言 <span className="wish-optional">选填</span><textarea disabled={pending} maxLength={200} onChange={(event) => setParentNote(event.target.value)} placeholder="写下这次兑现的温暖瞬间" value={parentNote} /></label>
        <label>孩子留言 <span className="wish-optional">选填</span><textarea disabled={pending} maxLength={200} onChange={(event) => setChildNote(event.target.value)} placeholder="也可以记录孩子想说的话" value={childNote} /></label>
        <div className="wish-redemption-upload">
          <div className="wish-image-picker-head"><div><strong>兑现照片 <span className="wish-required">必填</span></strong><small>支持 PNG、JPG 等图片，最多 3 张</small></div><span>{photos.length}/3</span></div>
          {photos.length > 0 ? <div className="wish-redemption-photo-grid">{photos.map((photo, index) => <div className="wish-redemption-photo" key={`${photo.file.name}-${photo.file.lastModified}`}><img alt={`待上传兑现照片 ${index + 1}`} src={photo.previewUrl} /><button aria-label={`删除第 ${index + 1} 张照片`} disabled={pending} onClick={() => removePhoto(index)} type="button"><X size={14} /></button></div>)}</div> : null}
          {photos.length < 3 ? <button className="secondary-button" disabled={pending} onClick={() => inputRef.current?.click()} type="button"><ImagePlus size={16} />选择照片</button> : null}
          <input accept="image/*" className="visually-hidden" disabled={pending} multiple onChange={addPhotos} ref={inputRef} type="file" />
        </div>
        {error ? <p aria-live="polite" className="wish-upload-status error">{error}</p> : null}
        {message ? <p aria-live="polite" className="wish-upload-status">{pending ? <Loader2 className="wish-spin" size={17} /> : <Check size={17} />}{message}</p> : null}
        <div className="wish-redemption-actions"><button className="secondary-button" disabled={pending} onClick={() => setOpen(false)} type="button">取消</button><button className="primary-button" disabled={pending} type="submit">{pending ? <Loader2 className="wish-spin" size={17} /> : <Check size={17} />}{pending ? "提交中…" : "保存兑现记录"}</button></div>
      </form>
    </section>
  );
}
