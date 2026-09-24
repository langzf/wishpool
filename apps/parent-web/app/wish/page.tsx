import { Camera, Gift, Heart, Images, RotateCcw, Sparkles, type LucideIcon } from "lucide-react";
import { createWishAction } from "@/app/actions";
import { WishCreateDialog } from "@/app/wish/WishCreateDialog";
import { WishImageRepair } from "@/app/wish/WishImageRepair";
import { WishRedemptionForm } from "@/app/wish/WishRedemptionForm";
import { FragmentGrid } from "@/components/FragmentGrid";
import { Shell } from "@/components/Shell";
import { loadWishData, type WishHistoryItem, type WishSummary } from "@/lib/dashboard-data";
import { requireParentPageContext } from "@/lib/parent-page";
import { renderParentPageFallback } from "@/lib/parent-page-render";

type PageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

const statusLabel: Record<string, { label: string; className: string }> = {
  draft: { label: "草稿", className: "status-waiting" },
  active: { label: "进行中", className: "status-blue" },
  unlocked: { label: "已集满", className: "status-ready" },
  redeemed: { label: "已兑现", className: "status-ready" },
  archived: { label: "未完成", className: "status-waiting" },
  cancelled: { label: "已取消", className: "status-waiting" },
  cancelled_by_parent: { label: "已取消", className: "status-waiting" }
};

export default async function WishPage({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const ctx = await requireParentPageContext();
  const fallback = renderParentPageFallback(ctx);
  if (fallback) return fallback;

  const data = ctx.data;
  if (!data) return null;
  const { currentWish, wishHistory } = loadWishData(data);
  const history = ensureCurrentInHistory(currentWish, wishHistory);
  const selectedHistoryStatus = singleParam(params.historyStatus) ?? "all";
  const filteredHistory = selectedHistoryStatus === "all"
    ? history
    : history.filter((item) => item.status === selectedHistoryStatus);
  const currentHistoryItem = history.find((item) => item.id === currentWish.id);
  const library = buildWishLibrary(history);
  const redeemed = history.filter((item) => item.status === "redeemed" || item.redemption);
  const photoCount = history.reduce((sum, item) => sum + (item.redemption?.photos.length ?? 0), 0);
  const currentTarget = Math.max(currentWish.targetFragments, 1);
  const remainingFragments = Math.max(currentTarget - currentWish.currentFragments, 0);

  return (
    <Shell>
      <header className="topbar">
        <div>
          <p className="muted">把本周努力变成孩子看得见的期待</p>
          <h1 className="page-title">心愿</h1>
        </div>
      </header>

      <section className="dashboard-grid wish-page" aria-label="心愿">
        {singleParam(params.actionError) ? <p className="form-error span-12">{singleParam(params.actionError)}</p> : null}
        {singleParam(params.actionSuccess) ? <p className="form-success span-12">{singleParam(params.actionSuccess)}</p> : null}

        <article className="wish-hero panel span-8">
          <div className="wish-section-heading">
            <span className="module-card-icon">
              <Heart size={20} aria-hidden="true" />
            </span>
            <div>
              <p className="muted">本周心愿</p>
              <h2>当前进行中</h2>
            </div>
          </div>
          <WishCardVisual
            currentFragments={currentWish.currentFragments}
            description={currentWish.description}
            fragmentCols={currentWish.fragmentCols}
            fragmentRows={currentWish.fragmentRows}
            fragmentVisualMode={currentWish.fragmentVisualMode}
            imageUrl={currentWish.imageUrl}
            litIndexes={currentWish.litIndexes}
            mask={currentWish.fragmentMask}
            status={currentWish.status}
            targetFragments={currentWish.targetFragments}
            title={currentWish.title}
          />
          {currentWish.id && !currentWish.imageUrl ? (
            <WishImageRepair familyId={data.child.familyId} childId={data.child.id} wishId={currentWish.id} note={currentWish.description} title={currentWish.title} />
          ) : null}
          {data.source === "api" && currentWish.status === "unlocked" && !currentHistoryItem?.redemption ? (
            <WishRedemptionForm childId={data.child.id} familyId={data.child.familyId} wishId={currentWish.id} />
          ) : null}
          <p className="wish-next-step">
            {currentWish.id
              ? remainingFragments === 0
                ? "碎片已经集满，可以安排兑现并留下照片记录。"
                : `还差 ${remainingFragments} 块碎片，核心任务完成并通过后会继续点亮。`
              : "还没有激活心愿，可以从右侧新建，或在下方复用历史心愿卡。"}
          </p>
        </article>

        <aside className="panel span-4 wish-create-panel">
          <p className="muted">把下一份期待放进本周计划</p>
          <h2>新建心愿卡</h2>
          {data.source === "api" ? (
            <WishCreateDialog
              childId={data.child.id}
              familyId={data.child.familyId}
              rewardMode={data.weeklyPlan.rewardMode}
              weekId={data.weeklyPlan.weekId}
            />
          ) : (
            <CoreApiUnavailable error={data.coreApiError} />
          )}
        </aside>

        <section className="wish-stat-grid span-12" aria-label="心愿统计">
          <StatCard icon={Sparkles} label="本周进度" value={`${Math.round((currentWish.currentFragments / currentTarget) * 100)}%`} />
          <StatCard icon={Gift} label="历史实现" value={`${redeemed.length} 次`} />
          <StatCard icon={Heart} label="可复用卡" value={`${library.length} 张`} />
          <StatCard icon={Images} label="留存照片" value={`${photoCount} 张`} />
        </section>

        <article className="panel span-12">
          <div className="wish-section-heading">
            <span className="module-card-icon">
              <RotateCcw size={20} aria-hidden="true" />
            </span>
            <div>
              <p className="muted">从喜欢的奖励再次出发</p>
              <h2>心愿卡库</h2>
            </div>
          </div>
          {library.length === 0 ? (
            <div className="empty-state section-subtitle">
              <Gift size={20} aria-hidden="true" />
              <div>
                <strong>还没有可复用心愿卡</strong>
                <p className="muted">创建并完成几次心愿后，常用奖励会沉淀在这里。</p>
              </div>
            </div>
          ) : (
            <div className="wish-library-grid">
              {library.map((item) => (
                <div className="wish-library-card" key={item.title}>
                  <WishMiniArt title={item.title} />
                  <div>
                    <h3>{item.title}</h3>
                    <p className="muted">{item.description}</p>
                    <div className="wish-card-meta">
                      <span className="status-pill status-ready">实现 {item.realizedCount} 次</span>
                      <span className="status-pill status-blue">{item.targetFragments} 碎片</span>
                    </div>
                  </div>
                  {data.source === "api" ? (
                    <form action={createWishAction}>
                      <input name="returnTo" type="hidden" value="/wish" />
                      <input name="childId" type="hidden" value={data.child.id} />
                      <input name="weekId" type="hidden" value={data.weeklyPlan.weekId} />
                      <input name="rewardMode" type="hidden" value={data.weeklyPlan.rewardMode} />
                      <input name="title" type="hidden" value={item.title} />
                      <input name="note" type="hidden" value={item.description} />
                      <input name="requiredFragments" type="hidden" value={item.targetFragments} />
                      {item.imageUrl ? <input name="imageMediaId" type="hidden" value={item.coverMediaId} /> : null}
                      <input name="fragmentVisualMode" type="hidden" value={item.fragmentVisualMode} />
                      <button className="secondary-button" type="submit">
                        <RotateCcw size={16} aria-hidden="true" />
                        再次许愿
                      </button>
                    </form>
                  ) : null}
                </div>
              ))}
            </div>
          )}
        </article>

        <article className="panel span-12">
          <div className="wish-section-heading">
            <span className="module-card-icon">
              <Camera size={20} aria-hidden="true" />
            </span>
            <div>
              <p className="muted">照片、文字和每一次兑现都会留在这里</p>
              <h2>历史心愿与留存</h2>
            </div>
          </div>
          <form className="wish-history-filters" method="get">
            <label htmlFor="history-status">筛选历史状态</label>
            <select id="history-status" name="historyStatus" defaultValue={selectedHistoryStatus}>
              <option value="all">全部</option>
              <option value="active">进行中</option>
              <option value="unlocked">已集满</option>
              <option value="redeemed">已兑现</option>
            </select>
            {selectedHistoryStatus !== "all" ? <a className="secondary-button" href="/wish">清除筛选</a> : null}
          </form>
          <div className="wish-history-list">
            {filteredHistory.length === 0 ? (
              <div className="empty-state">
                <Images size={20} aria-hidden="true" />
                <div>
                  <strong>还没有历史心愿</strong>
                  <p className="muted">第一张心愿卡创建后，会在这里持续记录进度和兑现瞬间。</p>
                </div>
              </div>
            ) : (
              filteredHistory.map((item) => <HistoryWishCard item={item} key={item.id} />)
            )}
          </div>
        </article>
      </section>
    </Shell>
  );
}

function WishCardVisual({
  title,
  description,
  status,
  currentFragments,
  targetFragments,
  imageUrl,
  fragmentVisualMode,
  fragmentRows,
  fragmentCols,
  mask,
  litIndexes
}: Readonly<{
  title: string;
  description: string;
  status: string;
  currentFragments: number;
  targetFragments: number;
  imageUrl?: string;
  fragmentVisualMode: string;
  fragmentRows?: number;
  fragmentCols?: number;
  mask?: WishSummary["fragmentMask"];
  litIndexes?: number[];
}>) {
  const statusMeta = statusLabel[status] ?? statusLabel.draft;
  return (
    <div className="wish-card-visual">
      <FragmentGrid
        cols={fragmentCols}
        current={currentFragments}
        description={description}
        imageUrl={imageUrl}
        litIndexes={litIndexes}
        mask={mask}
        mode={fragmentVisualMode}
        rows={fragmentRows}
        statusMeta={statusMeta}
        target={targetFragments}
        title={title}
      />
    </div>
  );
}

function WishMiniArt({ title, large = false }: Readonly<{ title: string; large?: boolean }>) {
  const seed = title.charCodeAt(0) % 3;
  const Icon = seed === 0 ? Gift : seed === 1 ? Sparkles : Heart;
  return (
    <div className={large ? "wish-mini-art wish-mini-art-large" : "wish-mini-art"}>
      <Icon size={large ? 42 : 26} aria-hidden="true" />
    </div>
  );
}

function StatCard({ icon: Icon, label, value }: Readonly<{ icon: LucideIcon; label: string; value: string }>) {
  return (
    <article className="panel wish-stat-card">
      <Icon size={20} aria-hidden="true" />
      <strong>{value}</strong>
      <span className="muted">{label}</span>
    </article>
  );
}

function HistoryWishCard({ item }: Readonly<{ item: WishHistoryItem }>) {
  const statusMeta = statusLabel[item.status] ?? statusLabel.draft;
  return (
    <details className="wish-history-card">
      <summary>
        <div className="wish-history-card-heading">
          <a href={`/wish/${encodeURIComponent(item.id)}`}>
            <strong>{item.title}</strong>
          </a>
          <p className="muted">
            {item.weekId || "历史心愿"} · {item.currentFragments}/{item.targetFragments} 碎片
          </p>
        </div>
        <div className="task-actions">
          <span className={`status-pill ${statusMeta.className}`}>{statusMeta.label}</span>
          <span className="status-pill status-ready">实现 {item.realizedCount} 次</span>
        </div>
      </summary>
      <div className="wish-history-detail">
        <FragmentGrid
          cols={item.fragmentCols}
          current={item.currentFragments}
          description={item.description}
          imageUrl={item.imageUrl}
          litIndexes={item.litIndexes}
          mask={item.fragmentMask}
          mode={item.fragmentVisualMode}
          rows={item.fragmentRows}
          target={item.targetFragments}
          title={item.title}
        />
        {item.redemption ? (
          <>
            <div className="wish-timeline">
              <span className="status-pill status-ready">兑现于 {item.redemption.redeemedDate}</span>
              {item.redemption.parentNote ? <p>{item.redemption.parentNote}</p> : null}
              {item.redemption.childNote ? <p className="muted">孩子说：{item.redemption.childNote}</p> : null}
            </div>
            <PhotoWall photos={item.redemption.photos} />
          </>
        ) : (
          <div className="empty-state">
            <Camera size={20} aria-hidden="true" />
            <div>
              <strong>暂无兑现留存</strong>
              <p className="muted">心愿兑现时可上传照片和文字，形成孩子的历史回忆。</p>
            </div>
          </div>
        )}
      </div>
    </details>
  );
}

function PhotoWall({ photos }: Readonly<{ photos: NonNullable<WishHistoryItem["redemption"]>["photos"] }>) {
  if (photos.length === 0) {
    return (
      <div className="wish-photo-wall">
        <div className="wish-photo-placeholder">
          <Images size={24} aria-hidden="true" />
          <span>兑现照片待补充</span>
        </div>
      </div>
    );
  }
  return (
    <div className="wish-photo-wall">
      {photos.map((photo) =>
        photo.downloadUrl ? (
          <img alt="心愿兑现照片" key={photo.id} src={photo.downloadUrl} />
        ) : (
          <div className="wish-photo-placeholder" key={photo.id}>
            <Images size={24} aria-hidden="true" />
            <span>{photo.contentType}</span>
          </div>
        )
      )}
    </div>
  );
}

function CoreApiUnavailable({ error }: Readonly<{ error?: string }>) {
  return (
    <div className="empty-state section-subtitle">
      <Gift size={20} aria-hidden="true" />
      <div>
        {error ? (
          <>
            <strong>数据加载失败: {error}</strong>
            <p className="muted">请稍后重试或检查服务状态。</p>
          </>
        ) : (
          <>
            <strong>样例数据暂不支持创建</strong>
            <p className="muted">连接 Core API 后可创建并激活新的心愿。</p>
          </>
        )}
      </div>
    </div>
  );
}

function ensureCurrentInHistory(
  currentWish: WishSummary,
  history: WishHistoryItem[]
) {
  if (!currentWish.id || history.some((item) => item.id === currentWish.id)) return history;
  return [
    {
      ...currentWish,
      weekId: "",
      realizedCount: 0
    },
    ...history
  ];
}

function buildWishLibrary(history: WishHistoryItem[]) {
  const cards = new Map<string, WishHistoryItem>();
  history.forEach((item) => {
    const key = item.title.trim().toLowerCase();
    if (!key) return;
    const existing = cards.get(key);
    if (!existing || item.realizedCount > existing.realizedCount || item.status === "redeemed") {
      cards.set(key, item);
    }
  });
  return Array.from(cards.values()).slice(0, 6);
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
