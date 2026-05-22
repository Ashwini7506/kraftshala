package ai.kraftshala.attendance.auth

import ai.kraftshala.attendance.data.RolePref
import ai.kraftshala.attendance.data.SupabaseClientProvider
import ai.kraftshala.attendance.domain.models.User
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.OTP
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val errorMessage: String? = null,
    val magicLinkSent: Boolean = false,
    val selectedRole: String = "student"
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(AuthUiState(selectedRole = RolePref.get(app)))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val supabase = SupabaseClientProvider.client

    fun setSelectedRole(role: String) {
        RolePref.set(getApplication(), role)
        _state.update { it.copy(selectedRole = role) }
    }

    init {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collectLatest { status ->
                when (status) {
                    is SessionStatus.Authenticated -> loadCurrentUser()
                    is SessionStatus.NotAuthenticated -> _state.update { it.copy(user = null) }
                    else -> { /* loading/refresh, ignore */ }
                }
            }
        }
    }

    fun sendMagicLink(email: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                supabase.auth.signInWith(OTP, redirectUrl = "kraftshala://auth/callback") {
                    this.email = email
                }
                _state.update { it.copy(isLoading = false, magicLinkSent = true) }
            } catch (e: Exception) {
                Timber.e(e, "magic link failed")
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun signOut(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try { supabase.auth.signOut() } catch (e: Exception) { Timber.w(e, "signOut") }
            RolePref.set(getApplication(), "student")
            _state.update { AuthUiState(selectedRole = "student") }
            onDone()
        }
    }

    private suspend fun loadCurrentUser() {
        try {
            val sessionUser = supabase.auth.currentUserOrNull() ?: return
            var profile = supabase.from("users").select {
                filter { eq("id", sessionUser.id) }
                limit(1)
            }.decodeSingleOrNull<User>()

            val selectedRole = RolePref.get(getApplication())
            if (profile == null) {
                // First sign-in for this auth user — provision a profile row
                val email = sessionUser.email ?: ""
                val name = email.substringBefore("@").ifBlank { "Kraftshala user" }
                profile = supabase.from("users").insert(
                    mapOf(
                        "id" to sessionUser.id,
                        "full_name" to name,
                        "email" to email,
                        "role" to selectedRole
                    )
                ) { select() }.decodeSingleOrNull<User>()
            } else if (profile.role != selectedRole) {
                // Role selection screen is the source of truth — sync DB row to match.
                profile = supabase.from("users").update(
                    mapOf("role" to selectedRole)
                ) {
                    select()
                    filter { eq("id", sessionUser.id) }
                }.decodeSingleOrNull<User>() ?: profile
            }
            _state.update { it.copy(user = profile) }
        } catch (e: Exception) {
            Timber.e(e, "load user profile")
            _state.update { it.copy(errorMessage = e.message) }
        }
    }
}
