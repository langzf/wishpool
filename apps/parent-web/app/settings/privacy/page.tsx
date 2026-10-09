import { ShieldCheck } from "lucide-react";
import { PrivacyActions } from "@/app/settings/privacy/PrivacyActions";
import { Shell } from "@/components/Shell";
import { requireParentPageContext } from "@/lib/parent-page";
import { renderParentPageFallback } from "@/lib/parent-page-render";
import { coreGetJson } from "@/lib/core-client";
import { getPrivacyStatusLabel } from "@/app/settings/privacy/status-labels";

type PrivacyRequest = { id: string; requestType: "export" | "delete"; status: string; createdAt: string; exportMedia?: { downloadUrl?: string | null } | null };

export default async function PrivacySettingsPage() {
  const ctx = await requireParentPageContext();
  const fallback = renderParentPageFallback(ctx);
  if (fallback) return fallback;
  if (!ctx.profile || !ctx.session) return null;
  const family = ctx.profile.families.find((item) => item.family.id === ctx.session?.familyId) ?? ctx.profile.families[0];
  if (!family) return <Shell><p className="form-error">当前没有可用的家庭信息。</p></Shell>;
  const isOwner = family.member.role === "parent_owner";
  const response = await coreGetJson<{ requests: PrivacyRequest[] }>(`/families/${family.family.id}/privacy-requests`, ctx.session.accessToken);
  const requests = response.requests;
  return <Shell>
    <header className="topbar"><div><p className="muted">管理家庭数据的导出与删除</p><h1 className="page-title">隐私设置</h1></div><ShieldCheck size={34} aria-hidden="true" /></header>
    <section className="dashboard-grid" aria-label="隐私设置">
      <article className="panel span-12"><h2>家庭数据权限</h2><p className="muted">当前家庭：{family.family.name}。当前角色：{family.member.role}。{isOwner ? " parent_owner 可见并可执行以下高风险操作。" : " 你不是家庭所有者，仅可查看说明。"}</p></article>
      <article className="panel span-12"><h2>隐私请求记录</h2>{requests.length === 0 ? <p className="muted">暂无隐私请求。</p> : <div className="privacy-request-list">{requests.map((item) => <div className="privacy-request-row" key={item.id}><span>{item.requestType === "export" ? "数据导出" : "家庭删除"}</span><span>{getPrivacyStatusLabel(item.status)}</span><time dateTime={item.createdAt}>{new Date(item.createdAt).toLocaleString("zh-CN")}</time>{item.exportMedia?.downloadUrl ? <a className="primary-button" href={item.exportMedia.downloadUrl}>下载数据</a> : null}</div>)}</div>}</article>
      {isOwner ? <div className="span-12"><PrivacyActions familyId={family.family.id} /></div> : <article className="panel span-12"><h2>仅家庭所有者可操作</h2><p className="muted">数据导出和家庭删除属于高风险操作，只有家庭所有者可以发起。</p></article>}
    </section>
  </Shell>;
}
