"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

const refreshEventTypes = new Set([
  "task.created",
  "task.skipped",
  "task.postponed",
  "submission.created",
  "review.approved",
  "review.needs_revision",
  "reward.issued",
  "wish.fragment_awarded",
  "wish.unlocked",
  "memory.weekly_card_generated",
  "room.item_unlocked",
  "room.item_arranged",
  "notification.created"
]);

export function RealtimeRefresh({ enabled }: Readonly<{ enabled: boolean }>) {
  const router = useRouter();

  useEffect(() => {
    if (!enabled) return undefined;
    let afterSeq = Number(window.localStorage.getItem("wishpool.parent.afterSeq") ?? "0");
    const source = new EventSource(`/api/realtime?afterSeq=${afterSeq}`);

    source.addEventListener("realtime.connected", (message) => {
      afterSeq = advanceCursor(message, afterSeq);
    });
    source.addEventListener("realtime.heartbeat", (message) => {
      afterSeq = advanceCursor(message, afterSeq);
    });
    source.addEventListener("family.event", (message) => {
      const parsed = parseMessage(message);
      const event = parsed?.event;
      if (!event || typeof event !== "object") return;
      const seq = Number((event as { seq?: number }).seq ?? afterSeq);
      if (Number.isFinite(seq) && seq > afterSeq) {
        afterSeq = seq;
        window.localStorage.setItem("wishpool.parent.afterSeq", String(afterSeq));
      }
      const eventType = (event as { type?: string }).type;
      if (eventType && refreshEventTypes.has(eventType)) router.refresh();
    });

    return () => source.close();
  }, [enabled, router]);

  return null;
}

function advanceCursor(message: MessageEvent, fallback: number) {
  const parsed = parseMessage(message);
  const latestSeq = Number(parsed?.latestSeq ?? fallback);
  if (!Number.isFinite(latestSeq) || latestSeq <= fallback) return fallback;
  window.localStorage.setItem("wishpool.parent.afterSeq", String(latestSeq));
  return latestSeq;
}

function parseMessage(message: MessageEvent) {
  try {
    return JSON.parse(message.data) as Record<string, unknown>;
  } catch {
    return null;
  }
}
