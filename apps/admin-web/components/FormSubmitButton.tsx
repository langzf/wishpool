"use client";

import { type ReactNode } from "react";
import { useFormStatus } from "react-dom";
import { Loader2, Save } from "lucide-react";

type FormSubmitButtonProps = {
  children: ReactNode;
  className?: string;
  disabled?: boolean;
  pendingText?: string;
};

export function FormSubmitButton({
  children,
  className = "button-secondary",
  disabled,
  pendingText = "保存中..."
}: FormSubmitButtonProps) {
  const { pending } = useFormStatus();

  return (
    <button className={className} disabled={disabled || pending} type="submit">
      {pending ? <Loader2 className="spin-icon" size={17} aria-hidden="true" /> : <Save size={17} aria-hidden="true" />}
      {pending ? pendingText : children}
    </button>
  );
}
