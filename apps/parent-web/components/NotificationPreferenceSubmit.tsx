"use client";

import { useFormStatus } from "react-dom";

export function NotificationPreferenceSubmit({ clear = false }: { clear?: boolean }) {
  const { pending } = useFormStatus();
  return (
    <button className={clear ? "secondary-button" : "primary-button"} disabled={pending} name={clear ? "clearQuietHours" : undefined} type="submit" value={clear ? "true" : undefined}>
      {pending ? "保存中…" : clear ? "清除安静时段" : "保存设置"}
    </button>
  );
}
