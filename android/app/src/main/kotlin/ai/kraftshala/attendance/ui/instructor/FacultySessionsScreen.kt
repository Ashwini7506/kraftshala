package ai.kraftshala.attendance.ui.instructor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.kraftshala.attendance.data.AttendanceRepository
import ai.kraftshala.attendance.domain.models.Session
import ai.kraftshala.attendance.ui.common.*
import ai.kraftshala.attendance.ui.student.FilterChips
import ai.kraftshala.attendance.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun FacultySessionsScreen(
    onSessionTap: (String) -> Unit,
    onHome: () -> Unit,
    onProfile: () -> Unit
) {
    val vm: FacultyViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { AttendanceRepository(context.applicationContext) }
    var filter by remember { mutableStateOf("All") }
    var attendancePctById by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(state.past) {
        scope.launch {
            val map = mutableMapOf<String, Int>()
            state.past.forEach { s ->
                val roster = repo.rosterForSession(s.id)
                if (roster.isNotEmpty()) {
                    val present = roster.count { it.finalFlag == "Present" || it.finalFlag == "PresentButLeftInBetween" }
                    map[s.id] = present * 100 / roster.size
                }
            }
            attendancePctById = map
        }
    }

    val avgPct = if (attendancePctById.isNotEmpty()) attendancePctById.values.average().toInt() else 0
    val totalTaught = state.past.size

    Column(modifier = Modifier.fillMaxSize().background(BgForm).statusBarsPadding()) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("Sessions", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("All sessions you've taught.", color = TextMuted, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))

            FilterChips(listOf("All", "This week", "This month", "By cohort"), filter) { filter = it }
            Spacer(Modifier.height(16.dp))

            WhiteCard {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("$totalTaught", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Sessions taught", color = TextMuted, fontSize = 12.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("$avgPct%", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Avg attendance", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            SectionLabel("RECENT")
            Spacer(Modifier.height(8.dp))

            if (state.past.isEmpty()) {
                WhiteCard { Text("No sessions yet.", color = TextMuted, fontSize = 14.sp) }
            } else {
                state.past.forEach { s ->
                    val pct = attendancePctById[s.id] ?: 0
                    SessionRow(s, pct) { onSessionTap(s.id) }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        BottomTabBar("sessions", onHome = onHome, onSessions = {}, onProfile = onProfile)
    }
}

@Composable
private fun SessionRow(s: Session, pct: Int, onClick: () -> Unit) {
    val pctColor = when {
        pct >= 85 -> SuccessText
        pct >= 70 -> WarningText
        else -> ErrorText
    }
    WhiteCard(onClick = onClick) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Class in ${s.classroom}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("$pct%", color = pctColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${formatDateShort(s.startAt)} • ${formatTime(s.startAt)} • Cohort ${s.cohortId.take(6)}",
                color = TextMuted, fontSize = 12.sp
            )
        }
    }
}
