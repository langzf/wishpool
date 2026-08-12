import { BookOpen, CheckSquare, Heart, Home, LayoutDashboard, Settings, Sparkles } from "lucide-react";

const navItems = [
  { label: "总览", href: "#overview", icon: LayoutDashboard, active: true },
  { label: "审核", href: "#reviews", icon: CheckSquare },
  { label: "计划", href: "#plan", icon: BookOpen },
  { label: "心愿", href: "#wish", icon: Heart },
  { label: "小屋", href: "#room", icon: Home },
  { label: "设置", href: "#settings", icon: Settings }
];

const mobileItems = navItems.slice(0, 4);

export function Shell({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="app-shell">
      <aside className="sidebar" aria-label="家长端导航">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            <Sparkles size={24} strokeWidth={2.4} />
          </span>
          WishPool
        </div>
        <nav className="nav-list">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <a className={`nav-item ${item.active ? "active" : ""}`} href={item.href} key={item.label}>
                <Icon size={20} aria-hidden="true" />
                <span>{item.label}</span>
              </a>
            );
          })}
        </nav>
      </aside>
      <main className="content">{children}</main>
      <nav className="mobile-nav" aria-label="家长端底部导航">
        {mobileItems.map((item) => {
          const Icon = item.icon;
          return (
            <a className={item.active ? "active" : ""} href={item.href} key={item.label}>
              <Icon size={20} aria-hidden="true" />
              <span>{item.label}</span>
            </a>
          );
        })}
      </nav>
    </div>
  );
}
