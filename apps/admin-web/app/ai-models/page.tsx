import { AlertCircle, CheckCircle2, Image, Route, Sparkles, Wand2 } from "lucide-react";
import { saveImageGenUsageAction } from "@/app/actions";
import { AiModelManager, type ImageModelProvider } from "@/components/AiModelManager";
import { AdminAuthPanel } from "@/components/AuthPanel";
import { AdminShell } from "@/components/AdminShell";
import { FormSubmitButton } from "@/components/FormSubmitButton";
import { requireAdminPageContext } from "@/lib/admin-page";
import { buildAdminHeaders } from "@/lib/runtime";
import { configForAdminWebSession } from "@/lib/session";

type PageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

type ImageGenUsage = {
  usageCode: string;
  providerCode: string;
  providerDisplayName?: string | null;
};

const usageOptions = [
  { code: "wish_card", label: "心愿卡封面", description: "生成家庭心愿的首图和分享图。", icon: Sparkles },
  { code: "memory_cover", label: "记忆封面", description: "为回忆相册生成封面图。", icon: Image },
  { code: "feedback_sticker", label: "反馈配图", description: "生成轻量反馈贴纸和提示插图。", icon: Wand2 }
];

export default async function AiModelsPage({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const { session } = await requireAdminPageContext();
  if (!session) {
    return <AdminAuthPanel error={singleParam(params.authError)} />;
  }

  const config = configForAdminWebSession(session);
  const [providers, usages] = await Promise.all([
    adminGet<ImageModelProvider[]>(`${config.adminApiBaseUrl}/admin/image-model-providers`, session.adminToken),
    adminGet<ImageGenUsage[]>(`${config.adminApiBaseUrl}/admin/image-gen-usages`, session.adminToken)
  ]);
  const enabledProviders = providers.filter((provider) => provider.isEnabled);
  const actionSuccess = singleParam(params.actionSuccess);
  const actionError = singleParam(params.actionError);

  return (
    <AdminShell>
      <header className="admin-topbar ai-models-hero">
        <div>
          <p className="section-kicker">Image Model Operations</p>
          <h1 className="admin-title">AI 模型中枢</h1>
          <p className="ai-models-lede">统一管理图片生成供应商、密钥状态、模型标识与业务默认路由。</p>
        </div>
        <div className="ai-models-hero-metrics" aria-label="模型运行概览">
          <span>
            <strong>{enabledProviders.length}</strong>
            启用节点
          </span>
          <span>
            <strong>{providers.length - enabledProviders.length}</strong>
            停用节点
          </span>
        </div>
      </header>

      {actionSuccess ? (
        <p className="form-success status-toast">
          <CheckCircle2 size={18} aria-hidden="true" />
          {actionSuccess}
        </p>
      ) : null}
      {actionError ? (
        <p className="form-error status-toast">
          <AlertCircle size={18} aria-hidden="true" />
          {actionError}
        </p>
      ) : null}

      <section className="admin-grid ai-models-page" aria-label="AI 模型管理">
        <AiModelManager enabledCount={enabledProviders.length} providers={providers} />

        <article className="panel span-12 usage-panel">
          <div className="section-header usage-section-header">
            <div>
              <p className="section-kicker">Routing Matrix</p>
              <h2>业务默认模型路由</h2>
              <p className="muted">未指定供应商时，业务会先使用这里的默认模型，再回落到全站默认模型。</p>
            </div>
            <span className="usage-section-mark" aria-hidden="true">
              <Route size={20} />
            </span>
          </div>
          <div className="usage-grid">
            {usageOptions.map((usage) => {
              const current = usages.find((item) => item.usageCode === usage.code);
              const Icon = usage.icon;
              return (
                <form action={saveImageGenUsageAction} className="usage-card" key={usage.code}>
                  <input name="usageCode" type="hidden" value={usage.code} />
                  <div className="usage-card-head">
                    <span className="usage-card-icon" aria-hidden="true">
                      <Icon size={19} />
                    </span>
                    <div>
                      <strong>{usage.label}</strong>
                      <p className="muted">{usage.description}</p>
                    </div>
                  </div>
                  <div className="usage-card-code">{usage.code}</div>
                  <div className="usage-current">
                    <span>当前模型</span>
                    <strong>{current?.providerDisplayName ?? current?.providerCode ?? "跟随全站默认"}</strong>
                  </div>
                  <select
                    disabled={enabledProviders.length === 0}
                    name="providerCode"
                    defaultValue={current?.providerCode ?? enabledProviders.find((provider) => provider.isDefault)?.code ?? enabledProviders[0]?.code ?? ""}
                  >
                    {enabledProviders.length === 0 ? <option value="">暂无可用模型</option> : null}
                    {enabledProviders.map((provider) => (
                      <option key={provider.code} value={provider.code}>{provider.displayName}</option>
                    ))}
                  </select>
                  <FormSubmitButton disabled={enabledProviders.length === 0} pendingText="保存中...">
                    保存路由
                  </FormSubmitButton>
                </form>
              );
            })}
          </div>
        </article>
      </section>
    </AdminShell>
  );
}

async function adminGet<T>(url: string, adminToken: string): Promise<T> {
  const response = await fetch(url, {
    cache: "no-store",
    headers: buildAdminHeaders(adminToken)
  });
  if (!response.ok) throw new Error(`Admin API ${response.status}: ${await response.text()}`);
  return (await response.json()) as T;
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
