import { serverClient } from "@/lib/supabase";
import type { AttendanceRecord } from "@/lib/types";

export default async function AnomaliesPage() {
  const supabase = serverClient();
  const { data: records = [] } = await supabase
    .from("attendance_records")
    .select("*")
    .in("final_flag", ["PresentButNotAtLecture", "PresentButLeftInBetween"])
    .order("finalised_at", { ascending: false })
    .limit(200);

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Anomalies</h1>
      <p className="text-gray-600">Students flagged as inconsistent across signals. Review and resolve.</p>
      <table className="w-full bg-white border rounded">
        <thead className="bg-gray-50">
          <tr>
            <Th>User</Th>
            <Th>Session</Th>
            <Th>Flag</Th>
            <Th>Self mark</Th>
            <Th>BLE throughout</Th>
            <Th>Instructor flag</Th>
          </tr>
        </thead>
        <tbody>
          {(records as AttendanceRecord[]).map((r) => (
            <tr key={r.id} className="border-t">
              <Td>{r.user_id.slice(0, 8)}</Td>
              <Td>{r.session_id.slice(0, 8)}</Td>
              <Td>{r.final_flag}</Td>
              <Td>{r.self_mark_at ? "✓" : "—"}</Td>
              <Td>{r.ble_detected_throughout ? "✓" : "—"}</Td>
              <Td>{r.instructor_called_absent ? "Called absent" : "—"}</Td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Th({ children }: { children: React.ReactNode }) {
  return <th className="text-left p-3 text-xs font-semibold uppercase text-gray-500">{children}</th>;
}
function Td({ children }: { children: React.ReactNode }) {
  return <td className="p-3 text-sm">{children}</td>;
}
