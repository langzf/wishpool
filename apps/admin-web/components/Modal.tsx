"use client";

import { type ReactNode, useEffect, useId, useState } from "react";
import { X } from "lucide-react";

type ModalProps = {
  children: ReactNode;
  description?: string;
  isOpen: boolean;
  onClose: () => void;
  title: string;
};

const closeAnimationMs = 180;

export function Modal({ children, description = "配置详情", isOpen, onClose, title }: ModalProps) {
  const generatedId = useId();
  const descriptionId = `${generatedId}-modal-description`;
  const [shouldRender, setShouldRender] = useState(isOpen);
  const [isLeaving, setIsLeaving] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setShouldRender(true);
      setIsLeaving(false);
      return;
    }

    if (!shouldRender) return;
    setIsLeaving(true);
    const timer = window.setTimeout(() => {
      setShouldRender(false);
      setIsLeaving(false);
    }, closeAnimationMs);
    return () => window.clearTimeout(timer);
  }, [isOpen, shouldRender]);

  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  if (!shouldRender) return null;

  return (
    <div
      aria-describedby={descriptionId}
      aria-label={title}
      aria-modal="true"
      className={isLeaving ? "modal-overlay is-leaving" : "modal-overlay"}
      onMouseDown={onClose}
      role="dialog"
    >
      <section className="modal-panel" onMouseDown={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div>
            <p className="muted" id={descriptionId}>{description}</p>
            <h2>{title}</h2>
          </div>
          <button aria-label="关闭弹窗" className="icon-button modal-close-button" onClick={onClose} type="button">
            <X size={20} aria-hidden="true" />
          </button>
        </div>
        <div className="modal-body">{children}</div>
      </section>
    </div>
  );
}
