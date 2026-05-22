package ai.kraftshala.attendance.data

import android.content.Context

object RolePref {
    private const val PREFS = "kraftshala_prefs"
    private const val KEY_ROLE = "selected_role"

    fun set(context: Context, role: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ROLE, role).apply()
    }

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ROLE, "student") ?: "student"
}
