import { ParentDashboard } from "@/components/Dashboard";
import { Shell } from "@/components/Shell";
import { loadParentDashboardData } from "@/lib/dashboard-data";

export default async function Page() {
  const data = await loadParentDashboardData();

  return (
    <Shell>
      <ParentDashboard data={data} />
    </Shell>
  );
}
