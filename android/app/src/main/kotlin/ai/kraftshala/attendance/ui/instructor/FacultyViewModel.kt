package ai.kraftshala.attendance.ui.instructor

import ai.kraftshala.attendance.ble.BleSessionService
import ai.kraftshala.attendance.data.AttendanceRepository
import ai.kraftshala.attendance.data.SupabaseClientProvider
import ai.kraftshala.attendance.domain.models.AttendanceRecord
import ai.kraftshala.attendance.domain.models.Session
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ai.kraftshala.attendance.domain.models.User
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FacultyUiState(
    val userId: String? = null,
    val userName: String = "Faculty",
    val today: List<Session> = emptyList(),
    val past: List<Session> = emptyList(),
    val activeSession: Session? = null,
    val roster: List<AttendanceRecord> = emptyList(),
    val nameMap: Map<String, String> = emptyMap(),
    val cohortStudents: List<User> = emptyList(),
    val isLoading: Boolean = false
)

class FacultyViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AttendanceRepository(app.applicationContext)
    private val client = SupabaseClientProvider.client

    private val _state = MutableStateFlow(FacultyUiState())
    val state: StateFlow<FacultyUiState> = _state.asStateFlow()

    private var rosterJob: Job? = null

    init {
        viewModelScope.launch {
            val u = client.auth.currentUserOrNull()
            if (u != null) {
                _state.update { it.copy(userId = u.id, userName = u.email?.substringBefore("@") ?: "Faculty") }
            }
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val uid = _state.value.userId
            val today = repo.todaySessions(instructorId = uid)
            val past = repo.pastSessions(instructorId = uid)
            val active = today.firstOrNull { it.status == "active" }
            _state.update { it.copy(today = today, past = past, activeSession = active, isLoading = false) }
        }
    }

    suspend fun loadSession(id: String): Session? = repo.sessionById(id)

    suspend fun rosterForSession(id: String): List<AttendanceRecord> = repo.rosterForSession(id)

    fun startSession(session: Session, onStarted: (Session) -> Unit) {
        viewModelScope.launch {
            val nonce = repo.startSessionNonce(session.id, session.instructorId) ?: session.sessionNonce
            val updated = session.copy(status = "active", sessionNonce = nonce)
            _state.update { it.copy(activeSession = updated) }

            val intent = Intent(getApplication(), BleSessionService::class.java).apply {
                putExtra(BleSessionService.EXTRA_NONCE, nonce)
                putExtra(BleSessionService.EXTRA_IS_INSTRUCTOR, true)
            }
            getApplication<Application>().startForegroundService(intent)
            subscribeRoster(updated.id)
            onStarted(updated)
        }
    }

    fun subscribeRoster(sessionId: String) {
        rosterJob?.cancel()
        rosterJob = viewModelScope.launch {
            // First, fetch the full cohort roster (all students in this session's cohort)
            val session = repo.sessionById(sessionId)
            val students = if (session != null) runCatching {
                client.from("users").select {
                    filter {
                        eq("cohort_id", session.cohortId)
                        eq("role", "student")
                    }
                }.decodeList<User>().sortedBy { it.fullName }
            }.getOrElse { emptyList() } else emptyList()
            val names = students.associate { it.id to it.fullName }
            _state.update { it.copy(cohortStudents = students, nameMap = names) }

            // Then keep roster live
            repo.rosterStream(sessionId).collect { list ->
                _state.update { it.copy(roster = list) }
            }
        }
    }

    fun overrideRecord(userId: String, kind: String, reason: String? = null) {
        val sid = _state.value.activeSession?.id ?: return
        viewModelScope.launch {
            repo.overrideRecord(sid, userId, kind, reason)
            _state.update { it.copy(roster = repo.rosterForSession(sid)) }
        }
    }

    fun endSession(onDone: () -> Unit) {
        val s = _state.value.activeSession ?: return
        viewModelScope.launch {
            repo.finaliseSession(s.id, s.instructorId)
            getApplication<Application>().stopService(Intent(getApplication(), BleSessionService::class.java))
            _state.update {
                it.copy(activeSession = it.activeSession?.copy(status = "closed"), roster = emptyList())
            }
            rosterJob?.cancel()
            onDone()
        }
    }
}
