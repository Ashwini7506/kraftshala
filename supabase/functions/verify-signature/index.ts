// Edge Function: verify-signature
// Verifies a student's signed attendance token against their stored public key.
// Writes attendance_records row + audit_log entry on success.

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.0";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

function b64ToBytes(b64: string): Uint8Array {
  const bin = atob(b64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes;
}

async function importPublicKey(pem: string): Promise<CryptoKey> {
  const b64 = pem
    .replace("-----BEGIN PUBLIC KEY-----", "")
    .replace("-----END PUBLIC KEY-----", "")
    .replace(/\s+/g, "");
  const der = b64ToBytes(b64);
  return crypto.subtle.importKey(
    "spki",
    der,
    { name: "ECDSA", namedCurve: "P-256" },
    false,
    ["verify"]
  );
}

serve(async (req) => {
  try {
    const {
      session_id,
      user_id,
      nonce,
      timestamp,
      ble_signal_strengths,
      signature_b64,
    } = await req.json();

    if (!session_id || !user_id || !nonce || !timestamp || !signature_b64) {
      return new Response(JSON.stringify({ error: "missing required fields" }), { status: 400 });
    }

    const supabase = createClient(SUPABASE_URL, SERVICE_ROLE, { db: { schema: "kraftshala" } });

    // Fetch user's active device public key
    const { data: device, error: devErr } = await supabase
      .from("devices")
      .select("public_key")
      .eq("user_id", user_id)
      .eq("status", "active")
      .single();

    if (devErr || !device) {
      return new Response(JSON.stringify({ error: "no active device" }), { status: 404 });
    }

    // Fetch session nonce
    const { data: session, error: sessErr } = await supabase
      .from("sessions")
      .select("id, session_nonce, status, end_at")
      .eq("id", session_id)
      .single();

    if (sessErr || !session) {
      return new Response(JSON.stringify({ error: "session not found" }), { status: 404 });
    }
    if (session.session_nonce !== nonce) {
      return new Response(JSON.stringify({ error: "nonce mismatch" }), { status: 401 });
    }

    // Build the message that should have been signed
    const message = JSON.stringify({
      session_id,
      nonce,
      timestamp,
      ble_signal_strengths,
    });
    const messageBytes = new TextEncoder().encode(message);

    // Verify signature
    const publicKey = await importPublicKey(device.public_key);
    const signature = b64ToBytes(signature_b64);
    const valid = await crypto.subtle.verify(
      { name: "ECDSA", hash: "SHA-256" },
      publicKey,
      signature,
      messageBytes
    );

    if (!valid) {
      return new Response(JSON.stringify({ error: "invalid signature" }), { status: 401 });
    }

    // Upsert attendance record
    const { error: upsertErr } = await supabase
      .from("attendance_records")
      .upsert(
        {
          session_id,
          user_id,
          self_mark_at: timestamp,
          signed_token: signature_b64,
          signature_verified: true,
        },
        { onConflict: "session_id,user_id" }
      );

    if (upsertErr) {
      return new Response(JSON.stringify({ error: upsertErr.message }), { status: 500 });
    }

    // Audit
    await supabase.from("audit_log").insert({
      actor_id: user_id,
      action: "attendance.self_mark",
      target_table: "attendance_records",
      target_id: null,
      payload: { session_id, timestamp, signal_strengths: ble_signal_strengths },
      row_hash: crypto.randomUUID(),
    });

    return new Response(JSON.stringify({ ok: true }), {
      headers: { "Content-Type": "application/json" },
    });
  } catch (e) {
    return new Response(JSON.stringify({ error: String(e) }), { status: 500 });
  }
});
