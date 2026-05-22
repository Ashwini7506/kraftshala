package ai.kraftshala.attendance.ui.student

import ai.kraftshala.attendance.ble.BleScanner
import ai.kraftshala.attendance.data.AttendanceRepository
import ai.kraftshala.attendance.data.SupabaseClientProvider
import ai.kraftshala.attendance.domain.models.AttendanceRecord
import ai.kraftshala.attendance.domain.models.Session
import ai.kraftshala.attendance.location.GeofenceManager
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ai.kraftshala.attendance.domain.models.User
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentUiState(
    val userId: String? = null,
    val userName: String = "Student",
    val cohortId: String? = null,
    val today: List<Session> = emptyList(),
    val past: List<Session> = emptyList(),
    val activeSession: Session? = null,
    val lastDetectionRssi: Int? = null,
    val bleDetected: Boolean = false,
    val inGeofence: Boolean = true,
    val markedAt: String? = null,
    val isLoading: Boolean = false,
    val lastError: String? = null
)

class StudentViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AttendanceRepository(app.applicationContext)
    private val scanner = BleScanner(app.applicationContext)
    private val geofence = GeofenceManager(app.applicationContext)
    private val client = SupabaseClientProvider.client

    private val _state = MutableStateFlow(StudentUiState())
    val state: StateFlow<StudentUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val u = client.auth.currentUserOrNull()
            if (u != null) {
                val profile = runCatching {
                    client.from("users").select {
                        filter { eq("id", u.id) }
                        limit(1)
                    }.decodeSingleOrNull<User>()
                }.getOrNull()
                _state.update {
                    it.copy(
                        userId = u.id,
                        userName = profile?.fullName?.substringBefore(" ") ?: (u.email?.substringBefore("@") ?: "Student"),
                        cohortId = profile?.cohortId
                    )
                }
            }
            refresh()
        }
        viewModelScope.launch {
            runCatching {
                scanner.scan().collect { detection ->
                    _state.update { it.copy(bleDetected = true, lastDetectionRssi = detection.rssi) }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val cohort = _state.value.cohortId
            val today = repo.todaySessions(cohortId = cohort)
            val past = repo.pastSessions(cohortId = cohort)
            val active = today.firstOrNull { it.status == "active" }
            _state.update { it.copy(today = today, past = past, activeSession = active, isLoading = false) }
        }
    }

    suspend fun loadSession(sessionId: String): Session? = repo.sessionById(sessionId)
    suspend fun myRecord(sessionId: String): AttendanceRecord? {
        val uid = _state.value.userId ?: return null
        return repo.myRecordFor(sessionId, uid)
    }

    fun markPresent(sessionId: String, onResult: (Boolean) -> Unit) {
        val s = _state.value
        val session = s.today.firstOrNull { it.id == sessionId } ?: s.activeSession ?: return
        val uid = s.userId ?: return
        viewModelScope.launch {
            val res = repo.markPresent(
                sessionId = session.id,
                userId = uid,
                nonce = session.sessionNonce,
                signalStrengths = listOfNotNull(s.lastDetectionRssi)
            )
            if (res.ok) {
                _state.update { it.copy(markedAt = java.time.Instant.now().toString(), lastError = null) }
            } else {
                _state.update { it.copy(lastError = res.errorMessage) }
            }
            onResult(res.ok)
        }
    }
}
