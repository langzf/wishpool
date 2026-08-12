import { Activity, Database, FileClock, Gauge, LockKeyhole, Settings, Shield } from "lucide-react";

const navItems = [
  { label: "控制台", href: "#overview", icon: Gauge, active: true },
  { label: "隐私", href: "#privacy", icon: LockKeyhole },
  { label: "队列", href: "#queues", icon: FileClock },
  { label: "存储", href: "#storage", icon: Database },
  { label: "审计", href: "#audit", icon: Shield },
  { label: "设置", href: "#settings", icon: Settings }
];

export function AdminShell({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="admin-shell">
      <aside className="admin-sidebar" aria-label="管理后台导航">
        <div className="admin-brand">
          <span aria-hidden="true">
            <Activity size={22} />
          </span>
          WishPool Ops
        </div>
        <nav className="admin-nav">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <a href={item.href} className={item.active ? "active" : ""} key={item.label}>
                <Icon size={19} aria-hidden="true" />
                <span>{item.label}</span>
              </a>
            );
          })}
        </nav>
      </aside>
      <main className="admin-main">{children}</main>
    </div>
  );
}
