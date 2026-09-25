import RoomBoard from "./RoomBoard";
import { Shell } from "@/components/Shell";
import { loadRoomData } from "@/lib/dashboard-data";
import { requireParentPageContext } from "@/lib/parent-page";
import { renderParentPageFallback } from "@/lib/parent-page-render";

type PageProps = { searchParams?: Promise<Record<string, string | string[] | undefined>> };

export default async function RoomPage({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const ctx = await requireParentPageContext();
  const fallback = renderParentPageFallback(ctx);
  if (fallback) return fallback;
  const data = ctx.data;
  if (!data) return null;
  const { roomState } = loadRoomData(data);
  return <Shell>
    <header className="topbar"><div><p className="muted">查看和调整 {data.child.nickname} 的成长小屋摆放</p><h1 className="page-title">小屋</h1></div></header>
    <section className="dashboard-grid" aria-label="小屋摆放预览">
      {singleParam(params.actionError) ? <p aria-live="assertive" className="form-error span-12" role="alert">{singleParam(params.actionError)}</p> : null}
      {singleParam(params.actionSuccess) ? <p aria-live="polite" className="form-success span-12" role="status">{singleParam(params.actionSuccess)}</p> : null}
      <article className="panel span-12"><h2>小屋摆放预览</h2><RoomBoard items={roomState.items} label={`${data.child.nickname} 的小屋`} /></article>
    </section>
  </Shell>;
}

function singleParam(value: string | string[] | undefined): string | undefined { return Array.isArray(value) ? value[0] : value; }
