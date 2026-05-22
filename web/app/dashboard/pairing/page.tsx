"use client";

import { useState } from "react";
import { browserClient } from "@/lib/supabase";

export default function PairingPage() {
  const [token, setToken] = useState<string | null>(null);
  const [expires, setExpires] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  async function generate() {
    setErr(null);
    const supabase = browserClient();
    const { data: session } = await supabase.auth.getSession();
    if (!session.session) { setErr("Not signed in"); return; }
    const res = await fetch(
      `${process.env.NEXT_PUBLIC_SUPABASE_URL}/functions/v1/qr-pairing`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${session.session.access_token}`
        },
        body: "{}"
      }
    );
    const json = await res.json();
    if (!res.ok) { setErr(json.error ?? "failed"); return; }
    setToken(json.qr_token);
    setExpires(json.expires_at);
  }

  return (
    <div className="space-y-6 max-w-xl">
      <h1 className="text-3xl font-bold">Device re-pairing</h1>
      <p className="text-gray-600">
        Lost or replaced your phone? Generate a one-time QR pairing code below and scan it from your new device.
      </p>

      {!token ? (
        <button
          onClick={generate}
          className="rounded bg-kraftshala-accent text-white px-4 py-2"
          style={{ background: "#0F62FE" }}
        >
          Generate pairing code
        </button>
      ) : (
        <div className="bg-white border rounded p-6 space-y-3">
          <p className="font-mono text-sm break-all">{token}</p>
          <p className="text-xs text-gray-500">Expires: {expires}</p>
          <p className="text-sm">
            On your new phone, open Kraftshala Attendance &gt; "Re-pair existing account" &gt; scan this code or paste the token.
          </p>
        </div>
      )}

      {err && <p className="text-red-600">{err}</p>}
    </div>
  );
}
