"use client";

import { type FormEvent, type ReactNode, useState } from "react";
import { useFormStatus } from "react-dom";
import { Cpu, Gauge, KeyRound, Link2, Loader2, Pencil, Plus, Power, Save, ShieldCheck, Sparkles, Star, Trash2, X } from "lucide-react";
import {
  deleteImageModelProviderAction,
  saveImageModelProviderAction,
  setDefaultImageModelProviderAction,
  toggleImageModelProviderAction
} from "@/app/actions";
import { Modal } from "@/components/Modal";

export type ImageModelProvider = {
  id: string;
  code: string;
  displayName: string;
  providerType: string;
  baseUrl: string;
  apiKeyMasked?: string | null;
  modelName: string;
  extraParams: Record<string, unknown>;
  isDefault: boolean;
  isEnabled: boolean;
  createdAt: string;
  updatedAt: string;
};

export const providerTypes = [
  ["volcengine_ark", "火山方舟 / Seedream"],
  ["aliyun_bailian", "阿里百炼 / 通义万相"],
  ["siliconflow", "SiliconFlow"],
  ["custom_openai_compatible", "OpenAI Images 兼容"],
  ["deterministic", "本地 deterministic"]
] as const;

type AiModelManagerProps = {
  enabledCount: number;
  providers: ImageModelProvider[];
};

export function AiModelManager({ enabledCount, providers }: AiModelManagerProps) {
  const [editingProvider, setEditingProvider] = useState<ImageModelProvider | null>(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [confirmingDeleteId, setConfirmingDeleteId] = useState<string | null>(null);
  const defaultProvider = providers.find((provider) => provider.isDefault);

  const closeModal = () => {
    setEditingProvider(null);
    setIsCreateOpen(false);
  };

  return (
    <article className="panel image-model-panel span-12">
      <div className="section-header image-model-section-header">
        <div>
          <p className="section-kicker">Image Provider Console</p>
          <h2>模型节点</h2>
          <p className="muted">密钥、URL、模型标识和扩展参数统一在弹窗中维护。</p>
        </div>
        <div className="admin-actions">
          <span className="pill pill-blue">
            <span className="pill-dot" aria-hidden="true" />
            {enabledCount}/{providers.length} 启用
          </span>
          <button className="button image-model-create-button" onClick={() => setIsCreateOpen(true)} type="button">
            <Plus size={20} aria-hidden="true" />
            新建模型
          </button>
        </div>
      </div>

      <div className="image-model-summary" aria-label="模型概览">
        <SummaryItem label="默认路由" value={defaultProvider?.displayName ?? "未设置"} />
        <SummaryItem label="启用节点" value={`${enabledCount} 个`} />
        <SummaryItem label="停用节点" value={`${providers.length - enabledCount} 个`} />
      </div>

      {providers.length === 0 ? (
        <div className="empty-state image-model-empty">
          <span className="empty-state-icon" aria-hidden="true">
            <KeyRound size={30} />
          </span>
          <div>
            <strong>暂无模型配置</strong>
            <p>先创建一个 deterministic 占位模型或真实供应商，再为业务场景指定默认模型。</p>
          </div>
          <button className="button image-model-create-button" onClick={() => setIsCreateOpen(true)} type="button">
            <Plus size={20} aria-hidden="true" />
            新建模型
          </button>
        </div>
      ) : (
        <div className="image-model-wall" role="list">
          {providers.map((provider, index) => (
            <section
              className={[
                "image-model-card",
                provider.isDefault ? "is-default" : "",
                provider.isEnabled ? "" : "is-disabled"
              ].join(" ").trim()}
              key={provider.id}
              role="listitem"
            >
              <div className="model-card-head">
                <div className="model-card-topline">
                  <span className={provider.isEnabled ? "status-light is-online" : "status-light"} aria-hidden="true" />
                  <span className="model-card-index">NODE {String(index + 1).padStart(2, "0")}</span>
                </div>
                <div className="model-card-badges">
                  <span className={provider.isEnabled ? "pill pill-green" : "pill pill-muted"}>
                    <span className="pill-dot" aria-hidden="true" />
                    {provider.isEnabled ? "运行中" : "已停用"}
                  </span>
                  {provider.isDefault ? <span className="pill pill-blue">默认路由</span> : null}
                </div>
              </div>

              <div className="image-model-card-main">
                <span className="image-model-mark" aria-hidden="true">
                  <Sparkles size={22} strokeWidth={2.2} />
                </span>
                <div className="image-model-identity">
                  <h3>{provider.displayName}</h3>
                  <p>{providerTypeLabel(provider.providerType)}</p>
                </div>
              </div>

              <div className="image-model-details">
                <DetailRow label="供应商编码" value={provider.code} icon={<ShieldCheck size={15} aria-hidden="true" />} />
                <DetailRow label="模型标识" value={provider.modelName} icon={<Cpu size={15} aria-hidden="true" />} />
                <DetailRow label="Base URL" value={provider.baseUrl} icon={<Link2 size={15} aria-hidden="true" />} />
                <DetailRow
                  label="密钥状态"
                  value={provider.apiKeyMasked ? provider.apiKeyMasked : "未配置"}
                  icon={<KeyRound size={15} aria-hidden="true" />}
                  tone={provider.apiKeyMasked ? "default" : "warning"}
                />
                <DetailRow
                  label="接口状态"
                  value={provider.isEnabled ? "可用" : "已停用"}
                  icon={<Gauge size={15} aria-hidden="true" />}
                  tone={provider.isEnabled ? "success" : "muted"}
                />
              </div>

              <div className="image-model-actions" aria-label={`${provider.displayName} 操作`}>
                {confirmingDeleteId === provider.id ? (
                  <div className="delete-confirm-inline">
                    <span>确认删除该模型？</span>
                    <div className="delete-confirm-actions">
                      <form action={deleteImageModelProviderAction}>
                        <input name="id" type="hidden" value={provider.id} />
                        <ActionButton danger idleIcon={<Trash2 size={18} aria-hidden="true" />} title="确认删除">
                          确认删除
                        </ActionButton>
                      </form>
                      <button className="button-secondary compact-button" onClick={() => setConfirmingDeleteId(null)} type="button">
                        <X size={16} aria-hidden="true" />
                        取消
                      </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <button
                      aria-label={`编辑 ${provider.displayName}`}
                      className="icon-button"
                      onClick={() => setEditingProvider(provider)}
                      title="编辑"
                      type="button"
                    >
                      <Pencil size={19} aria-hidden="true" />
                    </button>
                    <form action={setDefaultImageModelProviderAction}>
                      <input name="id" type="hidden" value={provider.id} />
                      <ActionIconButton
                        aria-label={`设为默认：${provider.displayName}`}
                        disabled={provider.isDefault || !provider.isEnabled}
                        idleIcon={<Star size={19} aria-hidden="true" />}
                        title="设为默认"
                      />
                    </form>
                    <form action={toggleImageModelProviderAction}>
                      <input name="id" type="hidden" value={provider.id} />
                      <input name="isEnabled" type="hidden" value={String(!provider.isEnabled)} />
                      <ActionIconButton
                        aria-label={`${provider.isEnabled ? "停用" : "启用"} ${provider.displayName}`}
                        disabled={provider.isDefault && provider.isEnabled}
                        idleIcon={<Power size={19} aria-hidden="true" />}
                        title={provider.isEnabled ? "停用" : "启用"}
                      />
                    </form>
                    <button
                      aria-label={`删除 ${provider.displayName}`}
                      className="icon-button danger"
                      disabled={provider.isDefault}
                      onClick={() => setConfirmingDeleteId(provider.id)}
                      title={provider.isDefault ? "默认模型不能删除" : "删除"}
                      type="button"
                    >
                      <Trash2 size={19} aria-hidden="true" />
                    </button>
                  </>
                )}
              </div>
            </section>
          ))}
        </div>
      )}

      <Modal isOpen={isCreateOpen} onClose={closeModal} title="新建模型" description="图片生成供应商配置">
        <ProviderForm />
      </Modal>
      <Modal isOpen={Boolean(editingProvider)} onClose={closeModal} title="编辑模型" description="图片生成供应商配置">
        {editingProvider ? <ProviderForm provider={editingProvider} /> : null}
      </Modal>
    </article>
  );
}

function SummaryItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="image-model-summary-item">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function DetailRow({
  icon,
  label,
  tone = "default",
  value
}: {
  icon?: ReactNode;
  label: string;
  tone?: "default" | "success" | "warning" | "muted";
  value: string;
}) {
  return (
    <div className={`image-model-detail-row tone-${tone}`}>
      <span>
        {icon}
        {label}
      </span>
      <strong>{value}</strong>
    </div>
  );
}

function ProviderForm({ provider }: { provider?: ImageModelProvider }) {
  const [extraParams, setExtraParams] = useState(
    JSON.stringify(provider?.extraParams ?? { size: "1024x1024", timeout_seconds: 25, max_retries: 2 }, null, 2)
  );
  const extraParamsError = validateExtraParams(extraParams);
  const isJsonValid = extraParamsError === null;

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    const submittedExtraParams = new FormData(event.currentTarget).get("extraParams");
    const submittedError = validateExtraParams(typeof submittedExtraParams === "string" ? submittedExtraParams : "");
    if (submittedError) {
      event.preventDefault();
      setExtraParams(typeof submittedExtraParams === "string" ? submittedExtraParams : "");
    }
  };

  return (
    <form action={saveImageModelProviderAction} className="admin-form image-provider-form" onSubmit={handleSubmit}>
      {provider ? <input name="id" type="hidden" value={provider.id} /> : null}
      <label>
        模型编码
        <input defaultValue={provider?.code ?? ""} disabled={Boolean(provider)} name="code" placeholder="seedream-5-pro" required={!provider} />
        <span className="field-hint">{provider ? "编码创建后不可修改，用于业务侧稳定引用。" : "建议使用小写字母、数字和连字符。"}</span>
      </label>
      <label>
        展示名称
        <input defaultValue={provider?.displayName ?? ""} name="displayName" placeholder="即梦 Seedream 5.0 Pro" required />
        <span className="field-hint">显示在管理端和业务默认模型选择器中。</span>
      </label>
      <label>
        供应商类型
        <select defaultValue={provider?.providerType ?? "volcengine_ark"} name="providerType">
          {providerTypes.map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </select>
        <span className="field-hint">决定后端适配器和默认参数解析方式。</span>
      </label>
      <label>
        Base URL
        <input defaultValue={provider?.baseUrl ?? ""} name="baseUrl" placeholder="https://ark.cn-beijing.volces.com" required />
        <span className="field-hint">填写供应商接口根地址，不包含具体模型路径。</span>
      </label>
      <label>
        API Key
        <input autoComplete="off" name="apiKey" placeholder={provider?.apiKeyMasked ? `${provider.apiKeyMasked}（留空不修改）` : "sk-..."} type="password" />
        <span className="field-hint">{provider?.apiKeyMasked ? "留空会保留现有密钥。" : "本地占位模型可留空。"}</span>
      </label>
      <label>
        模型标识
        <input defaultValue={provider?.modelName ?? ""} name="modelName" placeholder="doubao-seedream-5-0-pro-260628" required />
        <span className="field-hint">传给供应商的真实模型名称。</span>
      </label>
      <label className="extra-params-field">
        扩展参数 extra_params
        <textarea
          aria-invalid={!isJsonValid}
          onChange={(event) => setExtraParams(event.target.value)}
          value={extraParams}
          name="extraParams"
          rows={7}
          spellCheck={false}
        />
        <span className={isJsonValid ? "field-hint field-hint-success" : "field-hint field-hint-error"}>
          {isJsonValid ? "JSON 格式正确，会作为扩展参数提交。" : extraParamsError}
        </span>
      </label>
      <div className="checkbox-row">
        <label><input defaultChecked={provider?.isEnabled ?? true} name="isEnabled" type="checkbox" /> <span>启用</span></label>
        <label><input defaultChecked={provider?.isDefault ?? false} name="isDefault" type="checkbox" /> <span>全站默认</span></label>
      </div>
      <SubmitButton disabled={!isJsonValid} icon={<Save size={18} aria-hidden="true" />}>
        {provider ? "保存修改" : "创建模型"}
      </SubmitButton>
    </form>
  );
}

function providerTypeLabel(type: string): string {
  return providerTypes.find(([value]) => value === type)?.[1] ?? type;
}

function validateExtraParams(value: string): string | null {
  if (!value.trim()) return null;
  try {
    const parsed = JSON.parse(value);
    if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
      return "extra_params 必须是 JSON 对象，不能是数组或空值。";
    }
    return null;
  } catch {
    return "extra_params 不是合法 JSON，请检查逗号、引号和括号。";
  }
}

function ActionIconButton({
  "aria-label": ariaLabel,
  className = "",
  disabled,
  idleIcon,
  title
}: {
  "aria-label": string;
  className?: string;
  disabled?: boolean;
  idleIcon: ReactNode;
  title: string;
}) {
  const { pending } = useFormStatus();

  return (
    <button
      aria-label={ariaLabel}
      className={`icon-button ${className}`.trim()}
      disabled={disabled || pending}
      title={title}
      type="submit"
    >
      {pending ? <Loader2 className="spin-icon" size={19} aria-hidden="true" /> : idleIcon}
      <span className="sr-only">{title}</span>
    </button>
  );
}

function ActionButton({
  children,
  danger,
  idleIcon,
  title
}: {
  children: ReactNode;
  danger?: boolean;
  idleIcon: ReactNode;
  title: string;
}) {
  const { pending } = useFormStatus();

  return (
    <button className={danger ? "button-secondary compact-button danger" : "button-secondary compact-button"} disabled={pending} title={title} type="submit">
      {pending ? <Loader2 className="spin-icon" size={17} aria-hidden="true" /> : idleIcon}
      {pending ? "处理中..." : children}
    </button>
  );
}

function SubmitButton({ children, disabled, icon }: { children: ReactNode; disabled?: boolean; icon: ReactNode }) {
  const { pending } = useFormStatus();

  return (
    <button className="button form-submit-button" disabled={disabled || pending} type="submit">
      {pending ? <Loader2 className="spin-icon" size={18} aria-hidden="true" /> : icon}
      {pending ? "保存中..." : children}
    </button>
  );
}
