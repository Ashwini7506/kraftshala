// Edge Function: qr-pairing
// Generates a one-time QR pairing token (10-min expiry) for device re-pairing.
// Enforces frequency caps: max 1 re-pair per 7 days.

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.0";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

serve(async (req) => {
  try {
    const url = new URL(req.url);
    const action = url.pathname.endsWith("/redeem") ? "redeem" : "generate";

    const supabase = createClient(SUPABASE_URL, SERVICE_ROLE, { db: { schema: "kraftshala" } });

    if (action === "generate") {
      // Caller must be authenticated; pull user_id from JWT
      const authHeader = req.headers.get("Authorization");
      if (!authHeader) {
        return new Response(JSON.stringify({ error: "unauthorised" }), { status: 401 });
      }
      const token = authHeader.replace("Bearer ", "");
      const { data: { user }, error: userErr } = await supabase.auth.getUser(token);
      if (userErr || !user) {
        return new Response(JSON.stringify({ error: "invalid token" }), { status: 401 });
      }

      // Check frequency cap (1 per 7 days)
      const sevenDaysAgo = new Date();
      sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
      const { count } = await supabase
        .from("device_pairings")
        .select("id", { count: "exact", head: true })
        .eq("user_id", user.id)
        .gte("request_at", sevenDaysAgo.toISOString());

      if ((count ?? 0) >= 1) {
        // Log fraud flag
        await supabase.from("audit_log").insert({
          actor_id: user.id,
          action: "pairing.frequency_violation",
          target_table: "device_pairings",
          payload: { reason: "more than 1 re-pair in 7 days" },
          row_hash: crypto.randomUUID(),
        });
        return new Response(
          JSON.stringify({ error: "frequency cap exceeded, contact coordinator" }),
          { status: 429 }
        );
      }

      // Generate QR token
      const qrBytes = new Uint8Array(24);
      crypto.getRandomValues(qrBytes);
      const qr_token = btoa(String.fromCharCode(...qrBytes));
      const expires = new Date();
      expires.setMinutes(expires.getMinutes() + 10);

      // Find current active device id
      const { data: oldDevice } = await supabase
        .from("devices")
        .select("id")
        .eq("user_id", user.id)
        .eq("status", "active")
        .single();

      // Insert pairing record
      const { data: pairing, error: pairErr } = await supabase
        .from("device_pairings")
        .insert({
          user_id: user.id,
          old_device_id: oldDevice?.id,
          qr_token,
          qr_expires_at: expires.toISOString(),
          request_ip: req.headers.get("x-forwarded-for") ?? null,
        })
        .select()
        .single();

      if (pairErr) {
        return new Response(JSON.stringify({ error: pairErr.message }), { status: 500 });
      }

      return new Response(
        JSON.stringify({ qr_token, expires_at: expires.toISOString(), pairing_id: pairing.id }),
        { headers: { "Content-Type": "application/json" } }
      );
    }

    // REDEEM action
    if (action === "redeem") {
      const { qr_token, new_public_key, new_device_fingerprint } = await req.json();
      if (!qr_token || !new_public_key || !new_device_fingerprint) {
        return new Response(JSON.stringify({ error: "missing fields" }), { status: 400 });
      }

      // Find pairing
      const { data: pairing, error: pErr } = await supabase
        .from("device_pairings")
        .select("*")
        .eq("qr_token", qr_token)
        .is("paired_at", null)
        .single();

      if (pErr || !pairing) {
        return new Response(JSON.stringify({ error: "invalid or used QR token" }), { status: 401 });
      }

      if (new Date(pairing.qr_expires_at) < new Date()) {
        return new Response(JSON.stringify({ error: "QR token expired" }), { status: 401 });
      }

      // Revoke old device
      if (pairing.old_device_id) {
        await supabase
          .from("devices")
          .update({ status: "revoked", revoked_at: new Date().toISOString() })
          .eq("id", pairing.old_device_id);
      }

      // Insert new device
      const { data: newDev, error: newErr } = await supabase
        .from("devices")
        .insert({
          user_id: pairing.user_id,
          public_key: new_public_key,
          device_fingerprint: new_device_fingerprint,
          status: "active",
        })
        .select()
        .single();

      if (newErr) {
        return new Response(JSON.stringify({ error: newErr.message }), { status: 500 });
      }

      // Update pairing record
      await supabase
        .from("device_pairings")
        .update({ new_device_id: newDev.id, paired_at: new Date().toISOString() })
        .eq("id", pairing.id);

      // Audit
      await supabase.from("audit_log").insert({
        actor_id: pairing.user_id,
        action: "device.repaired",
        target_table: "devices",
        target_id: newDev.id,
        payload: { old_device_id: pairing.old_device_id, qr_token },
        row_hash: crypto.randomUUID(),
      });

      return new Response(
        JSON.stringify({ ok: true, device_id: newDev.id }),
        { headers: { "Content-Type": "application/json" } }
      );
    }

    return new Response("not found", { status: 404 });
  } catch (e) {
    return new Response(JSON.stringify({ error: String(e) }), { status: 500 });
  }
});
