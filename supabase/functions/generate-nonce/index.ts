// Edge Function: generate-nonce
// Called by the instructor app at session start.
// Generates a fresh session nonce, stores it on the session row.

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.0";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

serve(async (req) => {
  try {
    const { session_id, instructor_id } = await req.json();

    if (!session_id || !instructor_id) {
      return new Response(
        JSON.stringify({ error: "session_id and instructor_id required" }),
        { status: 400, headers: { "Content-Type": "application/json" } }
      );
    }

    const supabase = createClient(SUPABASE_URL, SERVICE_ROLE, { db: { schema: "kraftshala" } });

    // Validate instructor owns this session
    const { data: session, error: sessionErr } = await supabase
      .from("sessions")
      .select("id, instructor_id, status")
      .eq("id", session_id)
      .single();

    if (sessionErr || !session) {
      return new Response(JSON.stringify({ error: "session not found" }), { status: 404 });
    }
    if (session.instructor_id !== instructor_id) {
      return new Response(JSON.stringify({ error: "unauthorised" }), { status: 403 });
    }

    // Generate cryptographically strong nonce
    const nonceBytes = new Uint8Array(32);
    crypto.getRandomValues(nonceBytes);
    const nonce = btoa(String.fromCharCode(...nonceBytes));

    // Update session with nonce + active status
    const { error: updateErr } = await supabase
      .from("sessions")
      .update({ session_nonce: nonce, status: "active" })
      .eq("id", session_id);

    if (updateErr) {
      return new Response(JSON.stringify({ error: updateErr.message }), { status: 500 });
    }

    return new Response(
      JSON.stringify({ session_id, nonce, broadcast_uuid: "00001815-0000-1000-8000-00805F9B34FB" }),
      { headers: { "Content-Type": "application/json" } }
    );
  } catch (e) {
    return new Response(JSON.stringify({ error: String(e) }), { status: 500 });
  }
});
