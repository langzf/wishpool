import { ArrowLeft, Camera, Images } from "lucide-react";
import { FragmentGrid, type FragmentMask } from "@/components/FragmentGrid";
import { Shell } from "@/components/Shell";
import { coreGetJson } from "@/lib/core-client";
import { loadWishData, type WishHistoryItem } from "@/lib/dashboard-data";
import { requireParentPageContext } from "@/lib/parent-page";
import { renderParentPageFallback } from "@/lib/parent-page-render";

type PageProps = { params: Promise<{ wishId: string }> };

type WishDetail = {
  id: string;
  childId: string;
  title: string;
  note?: string | null;
  weekId?: string | null;
  status: string;
  requiredFragments: number;
  earnedFragments: number;
  rewardMode?: string | null;
  imageMedia?: { downloadUrl?: string | null; contentType?: string | null } | null;
  fragmentVisual?: {
    mode?: string;
    rows?: number;
    cols?: number;
    mask?: FragmentMask | null;
    litIndexes?: number[] | null;
  } | null;
};

const statusLabels: Record<string, string> = {
  draft: "草稿",
  active: "进行中",
  unlocked: "已集满",
  redeemed: "已兑现"
};

const rewardModeLabels: Record<string, string> = { flexible: "灵活", strict: "严格" };

export default async function WishDetailPage({ params }: PageProps) {
  const { wishId } = await params;
  const ctx = await requireParentPageContext();
  const fallback = renderParentPageFallback(ctx);
  if (fallback) return fallback;
  if (!ctx.data) return null;

  const { wishHistory } = loadWishData(ctx.data);
  const historyItem = wishHistory.find((item) => item.id === wishId);
  let wish: WishDetail | null = null;
  let errorMessage = "";

  if (ctx.data.source === "api" && ctx.session?.accessToken) {
    try {
      wish = await coreGetJson<WishDetail>(`/wishes/${encodeURIComponent(wishId)}`, ctx.session.accessToken);
    } catch (error) {
      errorMessage = error instanceof Error ? error.message : "心愿详情暂时无法加载。";
    }
  } else if (historyItem) {
    wish = wishFromHistory(historyItem);
  }

  return (
    <Shell>
      <header className="topbar">
        <div>
          <a className="back-link" href="/wish"><ArrowLeft size={16} aria-hidden="true" />返回心愿</a>
          <p className="muted">心愿历史详情</p>
          <h1 className="page-title">{wish?.title ?? "心愿详情"}</h1>
        </div>
      </header>
      {!wish ? (
        <section className="panel empty-state" aria-label="心愿详情加载失败">
          <Images size={24} aria-hidden="true" />
          <div>
            <strong>暂时找不到这份心愿</strong>
            <p className="muted">{errorMessage || "请返回心愿历史，重新选择一份心愿。"}</p>
          </div>
        </section>
      ) : (
        <section className="dashboard-grid wish-page wish-detail-page" aria-label="心愿详情">
          <article className="panel span-8">
            <div className="wish-detail-heading">
              <div>
                <span className="status-pill status-blue">{statusLabels[wish.status] ?? wish.status}</span>
                <h2>{wish.title}</h2>
                <p className="muted">{wish.weekId || "未关联周计划"}</p>
              </div>
              <strong className="wish-detail-progress">{wish.earnedFragments}/{wish.requiredFragments} 碎片</strong>
            </div>
            <FragmentGrid
              current={wish.earnedFragments}
              target={wish.requiredFragments}
              description={wish.note ?? undefined}
              imageUrl={wish.imageMedia?.downloadUrl ?? undefined}
              litIndexes={wish.fragmentVisual?.litIndexes ?? undefined}
              mask={wish.fragmentVisual?.mask ?? undefined}
              mode={wish.fragmentVisual?.mode ?? "grid_reveal"}
              rows={wish.fragmentVisual?.rows}
              cols={wish.fragmentVisual?.cols}
              title={wish.title}
            />
          </article>

          <aside className="panel span-4 wish-detail-meta">
            <h2>心愿信息</h2>
            <dl>
              <div><dt>状态</dt><dd>{statusLabels[wish.status] ?? wish.status}</dd></div>
              <div><dt>所属周</dt><dd>{wish.weekId || "未关联周计划"}</dd></div>
              <div><dt>奖励方式</dt><dd>{rewardModeLabels[wish.rewardMode ?? ""] ?? "未说明"}</dd></div>
              <div><dt>心愿说明</dt><dd>{wish.note || "这份心愿还没有留下说明。"}</dd></div>
              <div><dt>心愿图片</dt><dd>{wish.imageMedia?.downloadUrl ? "已添加图片" : "这份心愿还没有图片。"}</dd></div>
            </dl>
            {wish.imageMedia?.downloadUrl ? <img className="wish-detail-image" src={wish.imageMedia.downloadUrl} alt={`${wish.title} 的心愿图片`} /> : null}
          </aside>

          <RedemptionDetail redemption={historyItem?.redemption} />
        </section>
      )}
    </Shell>
  );
}

function RedemptionDetail({ redemption }: Readonly<{ redemption: WishHistoryItem["redemption"] }>) {
  if (!redemption) {
    return <div className="panel span-12 empty-state"><Camera size={20} aria-hidden="true" /><div><strong>暂无兑现记录</strong><p className="muted">心愿兑现后，这里会显示日期、照片和留言。</p></div></div>;
  }
  return (
    <article className="panel span-12 wish-redemption-card">
      <div className="wish-redemption-heading"><div><strong>兑现记录</strong><p className="muted">兑现于 {redemption.redeemedDate}</p></div><span className="status-pill status-ready">已兑现</span></div>
      {redemption.parentNote ? <p>{redemption.parentNote}</p> : null}
      {redemption.childNote ? <p className="muted">孩子留言：{redemption.childNote}</p> : null}
      {redemption.photos.length > 0 ? <div className="wish-photo-wall">{redemption.photos.map((photo) => photo.downloadUrl ? <img key={photo.id} src={photo.downloadUrl} alt="心愿兑现照片" /> : null)}</div> : <p className="muted">暂无兑现照片。</p>}
    </article>
  );
}

function wishFromHistory(item: WishHistoryItem): WishDetail {
  return {
    id: item.id,
    childId: item.childId,
    title: item.title,
    note: item.description,
    weekId: item.weekId,
    status: item.status,
    requiredFragments: item.targetFragments,
    earnedFragments: item.currentFragments,
    imageMedia: item.imageUrl ? { downloadUrl: item.imageUrl } : null,
    fragmentVisual: { mode: item.fragmentVisualMode, rows: item.fragmentRows, cols: item.fragmentCols, mask: item.fragmentMask, litIndexes: item.litIndexes }
  };
}
