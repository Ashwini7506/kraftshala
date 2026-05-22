package ai.kraftshala.attendance.data

import ai.kraftshala.attendance.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientProvider {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                scheme = "kraftshala"
                host = "auth"
            }
            install(Postgrest) {
                // Kraftshala tables live in the `kraftshala` schema
                defaultSchema = "kraftshala"
            }
            install(Realtime)
            install(Functions)
        }
    }
}
