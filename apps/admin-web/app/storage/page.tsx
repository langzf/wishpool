import { ExternalLink } from "lucide-react";
import { grantMediaAccessAction } from "@/app/actions";
import { AdminAuthPanel } from "@/components/AuthPanel";
import { AdminShell } from "@/components/AdminShell";
import { requireAdminPageContext } from "@/lib/admin-page";
import { loadAdminStorageData } from "@/lib/dashboard-data";
import { getAdminRuntimeConfig } from "@/lib/runtime";

type PageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function StoragePage({ searchParams }: PageProps) {
  const params = (await searchParams) ?? {};
  const { session, data } = await requireAdminPageContext();
  if (!session || !data) {
    return <AdminAuthPanel error={singleParam(params.authError)} />;
  }

  const config = getAdminRuntimeConfig();
  const storageData = loadAdminStorageData(data);
  const mediaAccessUrl = singleParam(params.mediaAccessUrl);
  const mediaAccessExpiresAt = singleParam(params.mediaAccessExpiresAt);
  const mediaAccessAuditLogId = singleParam(params.mediaAccessAuditLogId);

  return (
    <AdminShell>
      <header className="admin-topbar">
        <div>
          <p className="muted">数据源：{storageData.source}</p>
          <h1 className="admin-title">存储与媒体</h1>
        </div>
      </header>

      <section className="admin-grid" aria-label="存储与媒体">
        <article className="panel span-6">
          <div className="section-header">
            <h2>对象存储</h2>
            <a className="button-secondary" href={config.minioConsoleUrl} rel="noreferrer" target="_blank">
              <ExternalLink size={18} aria-hidden="true" />
              打开 MinIO
            </a>
          </div>
          <p className="muted">MinIO 控制台</p>
          <strong>{config.minioConsoleUrl}</strong>
        </article>

        <article className="panel span-6">
          <h2>媒体处理</h2>
          <p className="muted">媒体处理完成率</p>
          <div className="bar-track" aria-label="媒体处理完成率 94%">
            <div className="bar-fill" style={{ width: "94%" }} />
          </div>
        </article>

        <article className="panel span-6" id="media-access">
          <h2>媒体授权</h2>
          {storageData.source === "fixture" ? (
            <p className="empty-state">当前为 fixture 数据源，已隐藏写操作。</p>
          ) : (
            <form action={grantMediaAccessAction} className="admin-form">
              <label>
                家庭 ID
                <input name="familyId" placeholder={data.families[0]?.id ?? "family uuid"} />
              </label>
              <label>
                媒体 ID
                <input name="mediaAssetId" placeholder="media uuid" />
              </label>
              <label>
                原因
                <input name="reason" placeholder="排查上传失败" />
              </label>
              <label>
                分钟
                <input defaultValue="30" max="120" min="5" name="expiresInMinutes" type="number" />
              </label>
              <button className="button" type="submit">
                创建授权
              </button>
            </form>
          )}
          {mediaAccessUrl ? (
            <p className="grant-result">
              <a href={mediaAccessUrl} rel="noreferrer" target="_blank">
                打开授权链接
              </a>
              {mediaAccessExpiresAt ? <span>有效期至 {mediaAccessExpiresAt}</span> : null}
              {mediaAccessAuditLogId ? <span>审计 {mediaAccessAuditLogId}</span> : null}
            </p>
          ) : null}
        </article>
      </section>
    </AdminShell>
  );
}

function singleParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
