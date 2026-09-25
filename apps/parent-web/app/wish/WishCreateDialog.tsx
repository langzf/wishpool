"use client";

import { Plus, X } from "lucide-react";
import { useEffect, useRef, useState, type ComponentProps, type MouseEvent } from "react";
import { WishCreateForm } from "@/app/wish/WishCreateForm";

export function WishCreateDialog(props: ComponentProps<typeof WishCreateForm>) {
  const [open, setOpen] = useState(false);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const titleRef = useRef<HTMLHeadingElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const requestCloseRef = useRef<() => void>(() => undefined);

  function requestClose() {
    if (hasUnsavedChanges && !window.confirm("当前有未保存的内容，关闭后将丢失；已生成的图片也无法找回，确定关闭吗？")) return;
    setHasUnsavedChanges(false);
    setOpen(false);
    window.requestAnimationFrame(() => triggerRef.current?.focus());
  }
  requestCloseRef.current = requestClose;

  useEffect(() => {
    if (!open) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") requestCloseRef.current();
    };
    window.addEventListener("keydown", closeOnEscape);
    window.requestAnimationFrame(() => titleRef.current?.focus());
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", closeOnEscape);
    };
  }, [open]);

  function closeOnBackdrop(event: MouseEvent<HTMLDivElement>) {
    if (event.target === event.currentTarget) requestClose();
  }

  return (
    <>
      <button aria-controls="wish-create-dialog" aria-haspopup="dialog" className="primary-button wish-create-trigger" onClick={() => { setHasUnsavedChanges(false); setOpen(true); }} ref={triggerRef} type="button">
        <Plus size={17} aria-hidden="true" /> 新建心愿
      </button>
      {open ? (
        <div className="wish-modal-backdrop" role="presentation" onMouseDown={closeOnBackdrop}>
          <section aria-describedby="wish-create-dialog-description" aria-labelledby="wish-create-dialog-title" aria-modal="true" className="wish-modal" id="wish-create-dialog" role="dialog">
            <header className="wish-modal-header">
              <div>
                <p className="wish-modal-kicker">为孩子设计下一份期待</p>
                <h2 id="wish-create-dialog-title" ref={titleRef} tabIndex={-1}>新建心愿</h2>
                <p className="wish-modal-description" id="wish-create-dialog-description">把一个期待变成全家一起完成的小目标。</p>
              </div>
              <button aria-label="关闭新建心愿" className="icon-button wish-modal-close" onClick={requestClose} type="button"><X size={18} /></button>
            </header>
            <WishCreateForm {...props} onDirtyChange={setHasUnsavedChanges} />
          </section>
        </div>
      ) : null}
    </>
  );
}
