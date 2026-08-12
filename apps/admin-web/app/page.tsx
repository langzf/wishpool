import { AdminDashboard } from "@/components/AdminDashboard";
import { AdminShell } from "@/components/AdminShell";
import { loadAdminDashboardData } from "@/lib/dashboard-data";

export default async function Page() {
  const data = await loadAdminDashboardData();

  return (
    <AdminShell>
      <AdminDashboard data={data} />
    </AdminShell>
  );
}
