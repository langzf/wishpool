import { Clock3 } from "lucide-react";
import Link from "next/link";
import { exportMemoryAction } from "@/app/actions";
import { Shell } from "@/components/Shell";
import { loadMemoriesData } from "@/lib/dashboard-data";
import { requireParentPageContext } from "@/lib/parent-page";
import { renderParentPageFallback } from "@/lib/parent-page-render";

type PageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function MemoriesPage({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const ctx = await requireParentPageContext();
  const fallback = renderParentPageFallback(ctx);
  if (fallback) return fallback;

  const data = ctx.data;
  if (!data) return null;
  const { memories } = loadMemoriesData(data);

  return (
    <Shell>
      <header className="topbar">
        <div>
          <p className="muted">回顾每周的高光记录和成长摘要</p>
          <h1 className="page-title">成长纪念册</h1>
        </div>
      </header>

      <section className="dashboard-grid" aria-label="成长纪念册">
        {singleParam(params.actionError) ? <p aria-live="assertive" className="form-error span-12" role="alert">{singleParam(params.actionError)}</p> : null}
        {singleParam(params.actionSuccess) ? <p aria-live="polite" className="form-success span-12" role="status">{singleParam(params.actionSuccess)}</p> : null}
        <article className="panel span-12">
          <div className="memory-list">
            {memories.length === 0 ? (
              <div className="empty-state">
                <Clock3 size={20} aria-hidden="true" />
                <div>
                  <strong>还没有成长周卡</strong>
                  <p className="muted">周末生成纪念册后，回顾和导出入口会出现在这里。</p>
                </div>
              </div>
            ) : (
              memories.map((memory) => (
                <div className="memory-row" key={memory.id}>
                  <div>
                    <Link href={`/memories/${encodeURIComponent(memory.id)}`}>
                      <strong>{memory.title}</strong>
                    </Link>
                    <p className="muted" style={{ margin: "6px 0 0" }}>
                      {memory.summary}
                    </p>
                  </div>
                  {data.source === "api" ? (
                    <form action={exportMemoryAction}>
                      <input name="returnTo" type="hidden" value="/memories" />
                      <input name="memoryId" type="hidden" value={memory.id} />
                      <input name="format" type="hidden" value="pdf" />
                      <button className="secondary-button" type="submit">
                        导出
                      </button>
                    </form>
                  ) : null}
                </div>
              ))
            )}
          </div>
        </article>
      </section>
    </Shell>
  );
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
