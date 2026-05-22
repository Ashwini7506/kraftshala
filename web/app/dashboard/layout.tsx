import Link from "next/link";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen">
      <aside className="w-56 bg-white border-r p-4 space-y-2">
        <div className="text-xl font-bold mb-6">Kraftshala</div>
        <NavLink href="/dashboard">Overview</NavLink>
        <NavLink href="/dashboard/sessions">Sessions</NavLink>
        <NavLink href="/dashboard/anomalies">Anomalies</NavLink>
        <NavLink href="/dashboard/audit">Audit log</NavLink>
        <NavLink href="/dashboard/pairing">Device pairing</NavLink>
      </aside>
      <main className="flex-1 p-8">{children}</main>
    </div>
  );
}

function NavLink({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <Link
      href={href}
      className="block px-3 py-2 rounded text-sm text-gray-700 hover:bg-gray-100"
    >
      {children}
    </Link>
  );
}
