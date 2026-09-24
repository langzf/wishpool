import type { Metadata, Viewport } from "next";
import { RealtimeRefresh } from "@/components/RealtimeRefresh";
import "./styles.css";

export const metadata: Metadata = {
  title: "WishPool 家长端",
  description: "家庭任务、审核、心愿和成长纪念册工作台"
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#F8FAFC"
};

export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="zh-CN">
      <body><RealtimeRefresh enabled />{children}</body>
    </html>
  );
}
