"use client";

import { useFormStatus } from "react-dom";

type Props = Readonly<{
  children: React.ReactNode;
  pendingLabel?: string;
  className?: string;
  disabled?: boolean;
  type?: "submit" | "button";
  name?: string;
  value?: string;
  "aria-label"?: string;
}>;

export function PendingButton({ children, pendingLabel = "提交中…", className, disabled = false, type = "submit", name, value, ...aria }: Props) {
  const { pending } = useFormStatus();
  const busy = pending || disabled;
  return (
    <button aria-busy={busy ? "true" : undefined} className={className} disabled={busy} name={name} type={type} value={value} {...aria}>
      {pending ? pendingLabel : children}
    </button>
  );
}
