"use client";

import { useState } from "react";
import { browserClient } from "@/lib/supabase";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  async function send() {
    setErr(null);
    const supabase = browserClient();
    const { error } = await supabase.auth.signInWithOtp({
      email,
      options: { emailRedirectTo: `${window.location.origin}/dashboard` }
    });
    if (error) setErr(error.message);
    else setSent(true);
  }

  return (
    <main className="flex min-h-screen items-center justify-center p-8">
      <div className="w-full max-w-md space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Kraftshala Attendance</h1>
          <p className="text-gray-600 mt-1">Sign in to the admin dashboard.</p>
        </div>
        {sent ? (
          <p className="rounded bg-green-50 text-green-700 p-4">
            Check your inbox for the sign-in link.
          </p>
        ) : (
          <>
            <input
              type="email"
              className="w-full border rounded px-3 py-2"
              placeholder="you@kraftshala.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <button
              onClick={send}
              className="w-full rounded bg-kraftshala-accent text-white py-2 hover:opacity-90"
              style={{ background: "#0F62FE" }}
            >
              Send sign-in link
            </button>
            {err && <p className="text-red-600">{err}</p>}
          </>
        )}
      </div>
    </main>
  );
}
