"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";

const cursorStorageKey = "wishpool.parent.afterSeq";
const reconnectDelays = [1000, 2000, 4000, 8000, 16000, 30000];
const refreshEventTypes = new Set([
  "task.created", "task.skipped", "task.postponed", "submission.created", "review.approved",
  "review.needs_revision", "reward.issued", "wish.fragment_awarded", "wish.unlocked",
  "memory.weekly_card_generated", "room.item_unlocked", "room.item_arranged", "notification.created"
]);

type ConnectionState = "connected" | "reconnecting" | "disconnected";
type FamilyEvent = { seq?: number; type?: string };
type SyncPullResponse = { events?: FamilyEvent[]; latestSeq?: number };

export function RealtimeRefresh({ enabled }: Readonly<{ enabled: boolean }>) {
  const router = useRouter();
  const [connectionState, setConnectionState] = useState<ConnectionState>(enabled ? "reconnecting" : "disconnected");
  const sourceRef = useRef<EventSource | null>(null);
  const reconnectTimerRef = useRef<number | null>(null);
  const reconnectAttemptRef = useRef(0);
  const mountedRef = useRef(false);
  const forceReconnectRef = useRef<() => void>(() => undefined);
  const hadConnectionFailureRef = useRef(false);

  const refreshForEvent = useCallback((event: FamilyEvent) => {
    if (typeof event.type === "string" && refreshEventTypes.has(event.type)) router.refresh();
  }, [router]);

  useEffect(() => {
    mountedRef.current = true;
    if (!enabled) {
      setConnectionState("disconnected");
      return () => { mountedRef.current = false; };
    }

    const clearReconnectTimer = () => {
      if (reconnectTimerRef.current !== null) {
        window.clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = null;
      }
    };
    const closeSource = () => {
      sourceRef.current?.close();
      sourceRef.current = null;
    };
    const scheduleReconnect = () => {
      if (!mountedRef.current || !navigator.onLine || reconnectTimerRef.current !== null) return;
      const delay = reconnectDelays[Math.min(reconnectAttemptRef.current, reconnectDelays.length - 1)];
      reconnectAttemptRef.current += 1;
      setConnectionState("reconnecting");
      reconnectTimerRef.current = window.setTimeout(() => {
        reconnectTimerRef.current = null;
        connect();
      }, delay);
    };
    const connect = () => {
      if (!mountedRef.current || sourceRef.current) return;
      setConnectionState("reconnecting");
      let pullStarted = false;
      const source = new EventSource(`/api/realtime?afterSeq=${readCursor()}`);
      sourceRef.current = source;
      const markConnected = () => {
        if (!mountedRef.current || sourceRef.current !== source) return;
        reconnectAttemptRef.current = 0;
        setConnectionState("connected");
        if (hadConnectionFailureRef.current) {
          hadConnectionFailureRef.current = false;
        }
        if (!pullStarted) {
          pullStarted = true;
          void pullMissedEvents(refreshForEvent);
        }
      };
      source.onopen = markConnected;
      source.addEventListener("realtime.connected", (message) => {
        advanceCursor(message);
        markConnected();
      });
      source.addEventListener("realtime.heartbeat", (message) => advanceCursor(message));
      source.addEventListener("family.event", (message) => {
        const event = parseMessage(message)?.event;
        if (!event || typeof event !== "object") return;
        advanceEventCursor(event as FamilyEvent);
        refreshForEvent(event as FamilyEvent);
      });
      source.onerror = () => {
        if (sourceRef.current !== source) return;
        source.close();
        sourceRef.current = null;
        hadConnectionFailureRef.current = true;
        setConnectionState(navigator.onLine ? "reconnecting" : "disconnected");
        scheduleReconnect();
      };
    };
    forceReconnectRef.current = () => {
      clearReconnectTimer();
      closeSource();
      reconnectAttemptRef.current = 0;
      connect();
    };

    const handleOnline = () => {
      clearReconnectTimer();
      reconnectAttemptRef.current = 0;
      closeSource();
      connect();
    };
    const handleOffline = () => {
      clearReconnectTimer();
      closeSource();
      setConnectionState("disconnected");
    };
    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);
    connect();
    return () => {
      mountedRef.current = false;
      clearReconnectTimer();
      closeSource();
      forceReconnectRef.current = () => undefined;
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
    };
  }, [enabled, refreshForEvent]);

  if (!enabled) return null;
  const connected = connectionState === "connected";
  const reconnecting = connectionState === "reconnecting";
  return (
    <div aria-live="polite" className={`realtime-status realtime-status-${connectionState}`} role="status">
      <span className="realtime-status-dot" aria-hidden="true" />
      <span>{connected ? "已连接" : reconnecting ? "重连中…" : "已断开"}</span>
      <button className="realtime-status-button" type="button" onClick={() => forceReconnectRef.current()}>手动重连</button>
    </div>
  );
}

async function pullMissedEvents(onEvent: (event: FamilyEvent) => void) {
  try {
    const beforeCursor = readCursor();
    const response = await fetch(`/api/sync/pull?afterSeq=${beforeCursor}&limit=500`, { cache: "no-store" });
    if (!response.ok) throw new Error(`补拉失败：${response.status}`);
    const result = (await response.json()) as SyncPullResponse;
    for (const event of result.events ?? []) onEvent(event);
    if (typeof result.latestSeq === "number" && Number.isFinite(result.latestSeq)) writeCursor(result.latestSeq);
  } catch (error) {
    console.warn("WishPool 实时事件补拉失败，将继续保持实时连接。", error);
  }
}

function readCursor() {
  const value = Number(window.localStorage.getItem(cursorStorageKey) ?? "0");
  return Number.isFinite(value) && value >= 0 ? value : 0;
}
function writeCursor(value: number) {
  if (value >= readCursor()) window.localStorage.setItem(cursorStorageKey, String(value));
}
function advanceCursor(message: MessageEvent) {
  const latestSeq = Number(parseMessage(message)?.latestSeq);
  if (Number.isFinite(latestSeq)) writeCursor(latestSeq);
}
function advanceEventCursor(event: FamilyEvent) {
  const seq = Number(event.seq);
  if (Number.isFinite(seq)) writeCursor(seq);
}
function parseMessage(message: MessageEvent) {
  try { return JSON.parse(message.data) as Record<string, unknown>; } catch { return null; }
}
