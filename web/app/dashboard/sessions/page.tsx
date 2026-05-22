import { serverClient } from "@/lib/supabase";
import type { Session } from "@/lib/types";
import { format } from "date-fns";

export default async function SessionsPage() {
  const supabase = serverClient();
  const { data: sessions = [] } = await supabase
    .from("sessions")
    .select("*")
    .order("start_at", { ascending: false })
    .limit(100);

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Sessions</h1>
      <table className="w-full bg-white border rounded">
        <thead className="bg-gray-50">
          <tr>
            <Th>Classroom</Th>
            <Th>Start</Th>
            <Th>End</Th>
            <Th>Status</Th>
          </tr>
        </thead>
        <tbody>
          {(sessions as Session[]).map((s) => (
            <tr key={s.id} className="border-t">
              <Td>{s.classroom}</Td>
              <Td>{format(new Date(s.start_at), "dd MMM, HH:mm")}</Td>
              <Td>{format(new Date(s.end_at), "HH:mm")}</Td>
              <Td>{s.status}</Td>
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
