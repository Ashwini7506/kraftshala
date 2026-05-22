import { serverClient } from "@/lib/supabase";
import type { AuditEntry } from "@/lib/types";
import { format } from "date-fns";

export default async function AuditPage() {
  const supabase = serverClient();
  const { data: entries = [] } = await supabase
    .from("audit_log")
    .select("*")
    .order("created_at", { ascending: false })
    .limit(200);

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Audit log</h1>
      <p className="text-gray-600">Immutable, hash-chained record of every action.</p>
      <table className="w-full bg-white border rounded">
        <thead className="bg-gray-50">
          <tr>
            <Th>When</Th>
            <Th>Actor</Th>
            <Th>Action</Th>
            <Th>Target</Th>
          </tr>
        </thead>
        <tbody>
          {(entries as AuditEntry[]).map((e) => (
            <tr key={e.id} className="border-t">
              <Td>{format(new Date(e.created_at), "dd MMM HH:mm:ss")}</Td>
              <Td>{e.actor_id?.slice(0, 8) ?? "system"}</Td>
              <Td className="font-mono text-xs">{e.action}</Td>
              <Td>{e.target_table}/{e.target_id?.slice(0, 8) ?? ""}</Td>
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
function Td({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return <td className={`p-3 text-sm ${className}`}>{children}</td>;
}
