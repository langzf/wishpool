import { ArrowRight, Gift } from "lucide-react";
import { arrangeRoomItemAction } from "@/app/actions";
import { Shell } from "@/components/Shell";
import { loadRoomData } from "@/lib/dashboard-data";
import { requireParentPageContext } from "@/lib/parent-page";
import { renderParentPageFallback } from "@/lib/parent-page-render";

type PageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function RoomPage({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const ctx = await requireParentPageContext();
  const fallback = renderParentPageFallback(ctx);
  if (fallback) return fallback;

  const data = ctx.data;
  if (!data) return null;
  const { roomState } = loadRoomData(data);

  return (
    <Shell>
      <header className="topbar">
        <div>
          <p className="muted">查看和调整 {data.child.nickname} 的成长小屋摆放</p>
          <h1 className="page-title">小屋</h1>
        </div>
      </header>

      <section className="dashboard-grid" aria-label="小屋摆放预览">
        {singleParam(params.actionError) ? <p className="form-error span-12">{singleParam(params.actionError)}</p> : null}
        {singleParam(params.actionSuccess) ? <p className="form-success span-12">{singleParam(params.actionSuccess)}</p> : null}
        <article className="panel span-12">
          <h2>小屋摆放预览</h2>
          <div className="room-preview room-preview-large" aria-label={`${data.child.nickname} 的小屋`}>
            {roomState.items.map((item) => (
              <div
                className="room-item"
                key={item.id}
                style={{ left: `${item.x}%`, top: `${item.y}%`, opacity: item.unlocked ? 1 : 0.62 }}
              >
                <Gift size={16} aria-hidden="true" />
                {item.name}
                {data.source === "api" ? (
                  <form action={arrangeRoomItemAction}>
                    <input name="returnTo" type="hidden" value="/room" />
                    <input name="itemId" type="hidden" value={item.id} />
                    <input name="x" type="hidden" value={nextRoomX(item.x)} />
                    <input name="y" type="hidden" value={nextRoomY(item.y)} />
                    <button aria-label={`${item.name} 移动摆放`} className="mini-icon-button" type="submit">
                      <ArrowRight size={14} aria-hidden="true" />
                    </button>
                  </form>
                ) : null}
              </div>
            ))}
          </div>
        </article>
      </section>
    </Shell>
  );
}

function nextRoomX(value: number) {
  return Math.round((value + 12) % 84);
}

function nextRoomY(value: number) {
  return Math.round(value > 70 ? 28 : value + 8);
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
