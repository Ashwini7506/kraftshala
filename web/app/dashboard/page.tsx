import { serverClient } from "@/lib/supabase";

export default async function DashboardHome() {
  const supabase = serverClient();

  const [{ count: cohortCount }, { count: sessionCount }, { count: anomalyCount }] =
    await Promise.all([
      supabase.from("cohorts").select("*", { count: "exact", head: true }),
      supabase.from("sessions").select("*", { count: "exact", head: true }),
      supabase.from("attendance_records")
        .select("*", { count: "exact", head: true })
        .in("final_flag", ["PresentButNotAtLecture", "PresentButLeftInBetween"])
    ]);

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Overview</h1>
      <div className="grid grid-cols-3 gap-6">
        <Stat label="Cohorts" value={cohortCount ?? 0} />
        <Stat label="Sessions" value={sessionCount ?? 0} />
        <Stat label="Open anomalies" value={anomalyCount ?? 0} />
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="bg-white rounded border p-6">
      <div className="text-sm text-gray-500">{label}</div>
      <div className="text-3xl font-bold mt-2">{value}</div>
    </div>
  );
}
