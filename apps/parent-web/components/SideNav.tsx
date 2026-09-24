"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Bell, BookOpen, CheckSquare, Heart, Home, LayoutDashboard, Settings, ShieldCheck } from "lucide-react";

const navItems = [
  { label: "总览", href: "/", icon: LayoutDashboard },
  { label: "审核", href: "/reviews", icon: CheckSquare },
  { label: "计划", href: "/plan", icon: BookOpen },
  { label: "心愿", href: "/wish", icon: Heart },
  { label: "小屋", href: "/room", icon: Home },
  { label: "通知", href: "/notifications", icon: Bell },
  { label: "家庭设置", href: "/settings/family", icon: Settings },
  { label: "隐私设置", href: "/settings/privacy", icon: ShieldCheck }
];

type SideNavProps = {
  mobile?: boolean;
};

export function SideNav({ mobile = false }: Readonly<SideNavProps>) {
  const pathname = usePathname();
  const items = mobile ? navItems.slice(0, 4) : navItems;

  return (
    <>
      {items.map((item) => {
        const Icon = item.icon;
        const active = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);
        const className = [mobile ? "" : "nav-item", active ? "active" : ""].filter(Boolean).join(" ");
        return (
          <Link className={className} href={item.href} key={item.label}>
            <Icon size={20} aria-hidden="true" />
            <span>{item.label}</span>
          </Link>
        );
      })}
    </>
  );
}
