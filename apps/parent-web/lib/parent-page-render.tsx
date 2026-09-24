import { ParentAuthPanel, ParentSetupPanel } from "@/components/AuthPanel";
import { Shell } from "@/components/Shell";
import type { ParentPageContext } from "@/lib/parent-page";

export function renderParentPageFallback(ctx: ParentPageContext) {
  if (!ctx.session) {
    return <ParentAuthPanel />;
  }

  if (ctx.profileError || !ctx.profile) {
    return <ParentServiceUnavailable error={ctx.profileError} />;
  }

  if (ctx.needsSetup) {
    return (
      <Shell>
        <ParentSetupPanel profile={ctx.profile} />
      </Shell>
    );
  }

  if (!ctx.data) {
    if (ctx.dataError) {
      return (
        <Shell>
          <section className="dashboard-grid" aria-label="数据加载状态">
            <article className="panel span-12">
              <div className="empty-state">
                <div>
                  <strong>服务暂时不可用，数据加载失败</strong>
                  <p className="muted">{ctx.dataError}</p>
                  <a className="secondary-button" href="">点击重试</a>
                </div>
              </div>
            </article>
          </section>
        </Shell>
      );
    }
    return <ParentServiceUnavailable error={ctx.dataError} />;
  }

  return null;
}

function ParentServiceUnavailable({ error }: Readonly<{ error?: string }>) {
  return (
    <Shell>
      <section className="dashboard-grid" aria-label="数据加载状态">
        <article className="panel span-12">
          <div className="empty-state">
            <div>
              <strong>服务暂时不可用，数据加载失败</strong>
              {error ? <p className="muted">{error}</p> : null}
              <a className="secondary-button" href="">点击重试</a>
            </div>
          </div>
        </article>
      </section>
    </Shell>
  );
}
