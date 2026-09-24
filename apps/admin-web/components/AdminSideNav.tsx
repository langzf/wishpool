"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { BrainCircuit, Database, FileClock, Gauge, GitBranch, Home, LockKeyhole, Shield } from "lucide-react";

const navItems = [
  { label: "控制台", href: "/", icon: Gauge },
  { label: "家庭", href: "/families", icon: Home },
  { label: "隐私", href: "/privacy", icon: LockKeyhole },
  { label: "审计", href: "/audit", icon: Shield },
  { label: "队列", href: "/queues", icon: FileClock },
  { label: "存储", href: "/storage", icon: Database },
  { label: "AI 模型", href: "/ai-models", icon: BrainCircuit },
  { label: "工作流", href: "/workflows", icon: GitBranch }
];

export function AdminSideNav() {
  const pathname = usePathname();

  return (
    <>
      {navItems.map((item) => {
        const Icon = item.icon;
        const active = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);
        return (
          <Link href={item.href} className={active ? "active" : ""} key={item.label}>
            <Icon size={20} strokeWidth={2.1} aria-hidden="true" />
            <span>{item.label}</span>
          </Link>
        );
      })}
    </>
  );
}
