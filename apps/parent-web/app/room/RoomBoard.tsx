"use client";

import { Gift } from "lucide-react";
import { useRef, useState, type KeyboardEvent, type PointerEvent } from "react";

type RoomItem = { id: string; kind: string; name: string; x: number; y: number; layer?: number; unlocked: boolean; visible?: boolean; unlockedAt?: string | null };
type Props = { items: RoomItem[]; label: string };
type Slot = { x: number; y: number; layer: 1 };

export const ROOM_SLOTS: Slot[] = Array.from({ length: 8 }, (_, row) => Array.from({ length: 12 }, (_, column) => ({ x: 4 + column * 8, y: 2 + row * 10, layer: 1 as const }))).flat();

export default function RoomBoard({ items, label }: Props) {
  const boardRef = useRef<HTMLDivElement>(null);
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [previewSlot, setPreviewSlot] = useState<Slot | null>(null);
  const [positions, setPositions] = useState<Record<string, Slot>>({});
  const [message, setMessage] = useState("");
  const [savingId, setSavingId] = useState<string | null>(null);

  function nearestSlot(x: number, y: number, current = ROOM_SLOTS[0]) { return ROOM_SLOTS.reduce((nearest, slot) => ((slot.x - x) ** 2 + (slot.y - y) ** 2 < (nearest.x - x) ** 2 + (nearest.y - y) ** 2 ? slot : nearest), current); }
  function slotFromPointer(clientX: number, clientY: number): Slot | null {
    const board = boardRef.current; if (!board) return null;
    const rect = board.getBoundingClientRect(); if (clientX < rect.left || clientX > rect.right || clientY < rect.top || clientY > rect.bottom) return null;
    return nearestSlot(((clientX - rect.left) / rect.width) * 100, ((clientY - rect.top) / rect.height) * 100);
  }
  function beginDrag(event: PointerEvent<HTMLDivElement>, itemId: string) { event.currentTarget.setPointerCapture(event.pointerId); setDraggingId(itemId); setMessage(""); setPreviewSlot(slotFromPointer(event.clientX, event.clientY)); event.currentTarget.dataset.roomDragMode = "pointer"; }
  function moveDrag(event: PointerEvent<HTMLDivElement>) { if (draggingId) setPreviewSlot(slotFromPointer(event.clientX, event.clientY)); }
  async function savePosition(itemId: string, slot: Slot, mode: "pointer" | "keyboard") {
    setSavingId(itemId); setPositions((current) => ({ ...current, [itemId]: slot })); setMessage("保存中…");
    const handle = document.querySelector<HTMLElement>(`[data-room-drag="${itemId}"]`); if (handle) handle.dataset.roomDragMode = mode;
    try { const response = await fetch(`/room/items/${itemId}/arrange`, { method: "POST", headers: { "Content-Type": "application/json", "Idempotency-Key": `parent-room-${itemId}-${Date.now()}` }, body: JSON.stringify({ position: slot }) }); if (!response.ok) throw new Error("invalid-position"); setMessage("小屋摆放已保存。"); }
    catch { setMessage("这个位置不是合法槽位，摆放未保存。"); setPositions((current) => { const next = { ...current }; delete next[itemId]; return next; }); }
    finally { setSavingId(null); }
  }
  async function finishDrag(event: PointerEvent<HTMLDivElement>, itemId: string) { if (draggingId !== itemId) return; const slot = slotFromPointer(event.clientX, event.clientY); setDraggingId(null); setPreviewSlot(null); if (!slot) { setMessage("请将物件放在小屋内的合法槽位上。"); return; } await savePosition(itemId, slot, "pointer"); }
  function moveByKeyboard(event: KeyboardEvent<HTMLDivElement>, item: RoomItem) {
    const current = positions[item.id] ?? { x: item.x, y: item.y, layer: (item.layer ?? 1) as 1 }; const index = ROOM_SLOTS.findIndex((slot) => slot.x === current.x && slot.y === current.y); const currentIndex = index >= 0 ? index : 0; let nextIndex = currentIndex;
    if (event.key === "ArrowRight") nextIndex = Math.min(currentIndex + 1, ROOM_SLOTS.length - 1); else if (event.key === "ArrowLeft") nextIndex = Math.max(currentIndex - 1, 0); else if (event.key === "ArrowDown") nextIndex = Math.min(currentIndex + 12, ROOM_SLOTS.length - 1); else if (event.key === "ArrowUp") nextIndex = Math.max(currentIndex - 12, 0); else if (event.key === "Enter" || event.key === " ") { event.preventDefault(); void savePosition(item.id, current, "keyboard"); return; } else return;
    event.preventDefault(); setPositions((all) => ({ ...all, [item.id]: ROOM_SLOTS[nextIndex] })); setPreviewSlot(ROOM_SLOTS[nextIndex]);
  }
  async function setVisibility(itemId: string, visible: boolean) { setSavingId(itemId); setMessage(visible ? "恢复中…" : "隐藏中…"); const response = await fetch(`/room/items/${itemId}/${visible ? "unhide" : "hide"}`, { method: "POST" }); setSavingId(null); setMessage(response.ok ? (visible ? "物件已恢复显示。" : "物件已隐藏，历史记录仍保留。") : "操作未完成，请稍后重试。"); if (response.ok) window.location.reload(); }

  return <div><div ref={boardRef} className="room-preview room-preview-large" aria-label={label}>
    {draggingId ? ROOM_SLOTS.map((slot) => <span className={`room-slot${previewSlot?.x === slot.x && previewSlot?.y === slot.y ? " room-slot-active" : ""}`} data-room-slot={`${slot.x}-${slot.y}-${slot.layer}`} key={`${slot.x}-${slot.y}`} style={{ left: `${slot.x}%`, top: `${slot.y}%` }} aria-label={`合法槽位 ${slot.x},${slot.y}`} />) : null}
    {items.map((item) => { const position = positions[item.id] ?? { x: item.x, y: item.y, layer: (item.layer ?? 1) as 1 }; const visible = item.visible ?? item.unlocked; return <div className={`room-item${visible ? "" : " room-item-hidden"}`} data-room-item={item.id} data-room-x={position.x} data-room-y={position.y} data-room-layer={position.layer} key={item.id} style={{ left: `${position.x}%`, top: `${position.y}%`, zIndex: position.layer }}>
      <div className="room-drag-handle" data-room-drag={item.id} data-room-drag-mode="pointer" onPointerDown={(event) => beginDrag(event, item.id)} onPointerMove={moveDrag} onPointerUp={(event) => void finishDrag(event, item.id)} onKeyDown={(event) => moveByKeyboard(event, item)} role="button" tabIndex={0} aria-label={`移动 ${item.name}`} aria-busy={savingId === item.id ? "true" : undefined}><Gift size={16} aria-hidden="true" />{item.name}</div>
      {item.unlockedAt ? <span className="room-unlocked-badge">新</span> : null}<button aria-busy={savingId === item.id ? "true" : undefined} className="mini-icon-button" disabled={savingId !== null} type="button" onClick={() => void setVisibility(item.id, !visible)}>{visible ? "隐藏" : "恢复"}</button>
    </div>; })}
  </div><p className="room-drag-hint">拖动物件到合法槽位即可保存位置；方向键移动，Enter 或空格保存。</p>{message ? <p className="room-feedback" aria-live="polite" role="status">{message}</p> : null}</div>;
}
