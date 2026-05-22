// Edge Function: finalise-session
// Called by the instructor app when they submit the roster at session end.
// Locks all attendance_records for the session, writes audit log entries.

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.0";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

interface OverridePayload {
  user_id: string;
  override: "force_present" | "force_absent";
  reason?: string;
}

serve(async (req) => {
  try {
    const { session_id, instructor_id, overrides } = await req.json();

    if (!session_id || !instructor_id) {
      return new Response(JSON.stringify({ error: "missing fields" }), { status: 400 });
    }

    const supabase = createClient(SUPABASE_URL, SERVICE_ROLE, { db: { schema: "kraftshala" } });

    // Validate instructor owns session
    const { data: session, error: sessErr } = await supabase
      .from("sessions")
      .select("id, instructor_id, cohort_id, status")
      .eq("id", session_id)
      .single();
    if (sessErr || !session || session.instructor_id !== instructor_id) {
      return new Response(JSON.stringify({ error: "unauthorised" }), { status: 403 });
    }
    if (session.status === "closed") {
      return new Response(JSON.stringify({ error: "session already closed" }), { status: 400 });
    }

    // Apply overrides
    if (Array.isArray(overrides)) {
      for (const o of overrides as OverridePayload[]) {
        await supabase
          .from("attendance_records")
          .upsert(
            {
              session_id,
              user_id: o.user_id,
              instructor_override: o.override,
              override_reason: o.reason ?? null,
              finalised_at: new Date().toISOString(),
            },
            { onConflict: "session_id,user_id" }
          );

        await supabase.from("audit_log").insert({
          actor_id: instructor_id,
          action: "attendance.override",
          target_table: "attendance_records",
          payload: o,
          row_hash: crypto.randomUUID(),
        });
      }
    }

    // Ensure every cohort student has an attendance record (mark Absent if missing)
    const { data: cohortStudents } = await supabase
      .from("users")
      .select("id")
      .eq("cohort_id", session.cohort_id)
      .eq("role", "student");

    if (cohortStudents) {
      for (const s of cohortStudents) {
        await supabase
          .from("attendance_records")
          .upsert(
            {
              session_id,
              user_id: s.id,
              finalised_at: new Date().toISOString(),
            },
            { onConflict: "session_id,user_id", ignoreDuplicates: false }
          );
      }
    }

    // Mark session as closed
    await supabase.from("sessions").update({ status: "closed" }).eq("id", session_id);

    await supabase.from("audit_log").insert({
      actor_id: instructor_id,
      action: "session.closed",
      target_table: "sessions",
      target_id: session_id,
      payload: { override_count: overrides?.length ?? 0 },
      row_hash: crypto.randomUUID(),
    });

    return new Response(JSON.stringify({ ok: true }), {
      headers: { "Content-Type": "application/json" },
    });
  } catch (e) {
    return new Response(JSON.stringify({ error: String(e) }), { status: 500 });
  }
});
