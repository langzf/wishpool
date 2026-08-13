import { CheckCircle2, KeyRound, LogIn, Plus, UserRound } from "lucide-react";
import {
  completeParentFamilySetupAction,
  loginParentAction,
  requestParentPhoneCodeAction,
  selectParentChildAction
} from "@/app/actions";
import type { ParentProfileData } from "@/lib/profile-data";

type AuthPanelProps = {
  phone?: string;
  verificationToken?: string;
  debugCode?: string;
  error?: string;
};

export function ParentAuthPanel({ phone, verificationToken, debugCode, error }: Readonly<AuthPanelProps>) {
  return (
    <main className="auth-page">
      <section className="auth-panel" aria-label="家长登录">
        <div className="auth-mark" aria-hidden="true">
          <UserRound size={30} />
        </div>
        <p className="muted">WishPool 家长端</p>
        <h1 className="page-title">登录家庭工作台</h1>
        {error ? <p className="form-error">{error}</p> : null}

        <form action={requestParentPhoneCodeAction} className="auth-form">
          <label>
            手机号
            <input defaultValue={phone} inputMode="tel" name="phoneNumber" required />
          </label>
          <button className="secondary-button" type="submit">
            <KeyRound size={18} aria-hidden="true" />
            获取验证码
          </button>
        </form>

        <form action={loginParentAction} className="auth-form">
          <input name="verificationToken" type="hidden" value={verificationToken ?? ""} />
          <input name="phoneNumber" type="hidden" value={phone ?? ""} />
          <label>
            验证码
            <input autoComplete="one-time-code" defaultValue={debugCode} inputMode="numeric" name="code" required />
          </label>
          {debugCode ? <p className="inline-hint">本地调试验证码：{debugCode}</p> : null}
          <button className="primary-button" disabled={!verificationToken} type="submit">
            <LogIn size={18} aria-hidden="true" />
            登录
          </button>
        </form>
      </section>
    </main>
  );
}

export function ParentSetupPanel({
  profile,
  error
}: Readonly<{
  profile: ParentProfileData;
  error?: string;
}>) {
  const hasFamilies = profile.families.length > 0;
  const selectedFamily = profile.families.find((family) => family.family.id === profile.selectedFamilyId) ?? profile.families[0];

  return (
    <section className="setup-grid" aria-label="家庭资料补全">
      <article className="panel setup-panel">
        <CheckCircle2 size={24} color="#059669" aria-hidden="true" />
        <p className="muted">已登录：{profile.userName}</p>
        <h1 className="page-title">选择家庭和孩子</h1>
        {error ? <p className="form-error">{error}</p> : null}
        {hasFamilies ? (
          <form action={selectParentChildAction} className="auth-form">
            <label>
              家庭
              <select defaultValue={selectedFamily?.family.id} name="familyId" required>
                {profile.families.map((context) => (
                  <option key={context.family.id} value={context.family.id}>
                    {context.family.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              孩子
              <select defaultValue={profile.selectedChildId} name="childId" required>
                {profile.families.flatMap((context) =>
                  context.children.map((child) => (
                    <option key={child.id} value={child.id}>
                      {context.family.name} / {child.nickname}
                    </option>
                  ))
                )}
              </select>
            </label>
            <button className="primary-button" type="submit">
              进入工作台
            </button>
          </form>
        ) : (
          <p className="muted">这个账号还没有家庭空间，先创建一个用于管理计划、审核和心愿。</p>
        )}
      </article>

      <article className="panel setup-panel">
        <Plus size={24} color="#2563EB" aria-hidden="true" />
        <h2>创建家庭空间</h2>
        <form action={completeParentFamilySetupAction} className="auth-form">
          <label>
            家庭名称
            <input defaultValue={hasFamilies ? selectedFamily?.family.name : "我的家庭"} name="familyName" required />
          </label>
          <label>
            时区
            <input defaultValue={selectedFamily?.family.timezone ?? "Asia/Shanghai"} name="timezone" required />
          </label>
          <label>
            孩子昵称
            <input name="childNickname" required />
          </label>
          <label>
            出生年份
            <input inputMode="numeric" name="birthYear" />
          </label>
          <label>
            小屋主题
            <select defaultValue="forest" name="roomTheme">
              <option value="forest">森林</option>
              <option value="ocean">海边</option>
              <option value="space">星空</option>
            </select>
          </label>
          <button className="secondary-button" type="submit">
            创建并进入
          </button>
        </form>
      </article>
    </section>
  );
}
