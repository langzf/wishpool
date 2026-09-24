import { Sparkles } from "lucide-react";
import { SideNav } from "@/components/SideNav";

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
          <SideNav />
        </nav>
      </aside>
      <main className="content">{children}</main>
      <nav className="mobile-nav" aria-label="家长端底部导航">
        <SideNav mobile />
      </nav>
    </div>
  );
}
