import { createBrowserClient, createServerClient } from "@supabase/ssr";
import { cookies } from "next/headers";

const SUPABASE_URL = process.env.NEXT_PUBLIC_SUPABASE_URL!;
const SUPABASE_ANON = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!;

// Kraftshala tables live in the `kraftshala` schema (shared Supabase project with Sunstone)
const DB_SCHEMA = "kraftshala";

export function browserClient() {
  return createBrowserClient(SUPABASE_URL, SUPABASE_ANON, {
    db: { schema: DB_SCHEMA }
  });
}

export function serverClient() {
  const cookieStore = cookies();
  return createServerClient(SUPABASE_URL, SUPABASE_ANON, {
    db: { schema: DB_SCHEMA },
    cookies: {
      get(name: string) { return cookieStore.get(name)?.value; },
      set(name, value, options) { cookieStore.set({ name, value, ...options }); },
      remove(name, options) { cookieStore.set({ name, value: "", ...options }); }
    }
  });
}
