package ai.kraftshala.attendance.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.kraftshala.attendance.data.AttendanceRepository
import ai.kraftshala.attendance.domain.models.AttendanceRecord
import ai.kraftshala.attendance.domain.models.Session
import ai.kraftshala.attendance.ui.common.*
import ai.kraftshala.attendance.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun StudentSessionsScreen(
    onSessionTap: (String) -> Unit,
    onHome: () -> Unit,
    onProfile: () -> Unit
) {
    val vm: StudentViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { AttendanceRepository(context.applicationContext) }
    var records by remember { mutableStateOf<Map<String, AttendanceRecord>>(emptyMap()) }

    LaunchedEffect(state.userId, state.past) {
        val uid = state.userId ?: return@LaunchedEffect
        scope.launch {
            val m = mutableMapOf<String, AttendanceRecord>()
            state.past.forEach { s ->
                repo.myRecordFor(s.id, uid)?.let { m[s.id] = it }
            }
            records = m
        }
    }

    var filter by remember { mutableStateOf("All") }
    val filteredPast = remember(filter, state.past, records) {
        when (filter) {
            "Missed" -> state.past.filter { records[it.id]?.finalFlag == "Absent" }
            else -> state.past
        }
    }

    val total = state.past.size
    val present = records.values.count { it.finalFlag == "Present" || it.finalFlag == "PresentButLeftInBetween" }
    val pct = if (total > 0) (present * 100 / total) else 0

    Column(modifier = Modifier.fillMaxSize().background(BgForm).statusBarsPadding()) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("Sessions", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("Your attendance record.", color = TextMuted, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))

            WhiteCard {
                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$present of $total", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Lectures attended", color = TextMuted, fontSize = 12.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$pct%", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Your attendance", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(InputFill)) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(pct / 100f).background(AccentPrimary))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            FilterChips(listOf("All", "This week", "This month", "Missed"), filter) { filter = it }

            Spacer(Modifier.height(20.dp))
            SectionLabel("RECENT")
            Spacer(Modifier.height(8.dp))

            if (filteredPast.isEmpty()) {
                WhiteCard { Text("No sessions yet.", color = TextMuted, fontSize = 14.sp) }
            } else {
                filteredPast.forEach { s ->
                    SessionHistoryRow(s, records[s.id]) { onSessionTap(s.id) }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        BottomTabBar("sessions", onHome = onHome, onSessions = {}, onProfile = onProfile)
    }
}

@Composable
fun FilterChips(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScrollEnable()) {
        items.forEach { label ->
            val active = label == selected
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) AccentPrimary else InputFill)
                    .clickable { onSelect(label) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(label, color = if (active) SurfaceWhite else TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun Modifier.horizontalScrollEnable(): Modifier {
    val s = androidx.compose.foundation.rememberScrollState()
    return this.then(Modifier.horizontalScroll(s))
}

@Composable
private fun SessionHistoryRow(s: Session, rec: AttendanceRecord?, onClick: () -> Unit) {
    WhiteCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Class in ${s.classroom}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("${formatDateShort(s.startAt)} • ${formatTime(s.startAt)}", color = TextMuted, fontSize = 12.sp)
            }
            val (label, bg, fg) = when (rec?.finalFlag) {
                "Present" -> Triple("Present", SuccessBg, SuccessText)
                "PresentButLeftInBetween" -> Triple("Late", WarningBg, WarningText)
                "Absent", "PresentButNotAtLecture" -> Triple("Absent", ErrorBg, ErrorText)
                else -> Triple("—", InputFill, TextMuted)
            }
            StatusPill(label, bg, fg)
        }
    }
}
