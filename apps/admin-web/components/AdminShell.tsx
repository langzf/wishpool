import { Activity } from "lucide-react";
import { AdminSideNav } from "@/components/AdminSideNav";

export function AdminShell({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="admin-shell">
      <aside className="admin-sidebar" aria-label="管理后台导航">
        <div className="admin-brand">
          <span aria-hidden="true">
            <Activity size={22} />
          </span>
          <strong>WishPool Ops</strong>
        </div>
        <div className="admin-sidebar-status">本地治理中枢</div>
        <nav className="admin-nav">
          <AdminSideNav />
        </nav>
      </aside>
      <main className="admin-main">{children}</main>
    </div>
  );
}
