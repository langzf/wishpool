import { KeyRound, ShieldCheck } from "lucide-react";
import { loginAdminAction } from "@/app/actions";
import { getAdminRuntimeConfig } from "@/lib/runtime";

export function AdminAuthPanel({ error }: Readonly<{ error?: string }>) {
  const config = getAdminRuntimeConfig();

  return (
    <main className="admin-auth-page">
      <section className="admin-auth-panel" aria-label="管理后台登录">
        <div className="admin-auth-mark" aria-hidden="true">
          <ShieldCheck size={30} />
        </div>
        <p className="muted">WishPool 本地治理</p>
        <h1 className="admin-title">验证管理令牌</h1>
        <p className="muted">管理后台会校验 Admin API，令牌保存为本地 httpOnly cookie。</p>
        {error ? <p className="form-error">{error}</p> : null}
        <form action={loginAdminAction} className="admin-form">
          <label>
            Admin API
            <input defaultValue={config.adminApiBaseUrl} name="adminApiBaseUrl" readOnly />
          </label>
          <label>
            管理令牌
            <input autoComplete="off" name="adminToken" required type="password" />
          </label>
          <button className="button" type="submit">
            <KeyRound size={18} aria-hidden="true" />
            进入后台
          </button>
        </form>
      </section>
    </main>
  );
}
