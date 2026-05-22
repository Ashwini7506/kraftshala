package ai.kraftshala.attendance.ui.instructor

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.kraftshala.attendance.domain.models.AttendanceRecord
import ai.kraftshala.attendance.domain.models.Session
import ai.kraftshala.attendance.ui.common.*
import ai.kraftshala.attendance.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun FacultySessionDetailScreen(sessionId: String, onBack: () -> Unit) {
    val vm: FacultyViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<Session?>(null) }
    var roster by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }

    LaunchedEffect(sessionId) {
        scope.launch {
            session = vm.loadSession(sessionId)
            roster = vm.rosterForSession(sessionId)
        }
    }
    var activeFilter by remember { mutableStateOf("Present") }

    val counts = mapOf(
        "Present" to roster.count { it.finalFlag == "Present" },
        "Absent" to roster.count { it.finalFlag == "Absent" },
        "Not in room" to roster.count { it.finalFlag == "PresentButNotAtLecture" },
        "Left early" to roster.count { it.finalFlag == "PresentButLeftInBetween" }
    )
    val total = roster.size
    val present = counts["Present"] ?: 0
    val pct = if (total > 0) present * 100 / total else 0

    val filteredRoster = when (activeFilter) {
        "Present" -> roster.filter { it.finalFlag == "Present" }
        "Absent" -> roster.filter { it.finalFlag == "Absent" }
        "Not in room" -> roster.filter { it.finalFlag == "PresentButNotAtLecture" }
        "Left early" -> roster.filter { it.finalFlag == "PresentButLeftInBetween" }
        else -> roster
    }

    Column(modifier = Modifier.fillMaxSize().background(BgForm).statusBarsPadding().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(SurfaceWhite).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.weight(1f))
            StatusPill("CLOSED", InputFill, TextMuted)
        }
        Spacer(Modifier.height(20.dp))
        Text(session?.let { "Class in ${it.classroom}" } ?: "", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(
            session?.let { "${formatDateShort(it.startAt)} • ${timeRange(it.startAt, it.endAt)} • Room ${it.classroom}" } ?: "",
            color = TextMuted, fontSize = 13.sp
        )

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(20.dp))
            WhiteCard {
                Column {
                    Text("$present / $total", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Marked present", color = TextMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(InputFill)) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(pct / 100f).background(Warning))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().horizontalScrollEnable3()) {
                listOf("Present", "Absent", "Not in room", "Left early").forEach { label ->
                    val active = activeFilter == label
                    val c = counts[label] ?: 0
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (active) AccentPrimary else InputFill)
                            .clickable { activeFilter = label }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("$label $c", color = if (active) SurfaceWhite else TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (filteredRoster.isEmpty()) {
                WhiteCard { Text("Empty bucket.", color = TextMuted, fontSize = 14.sp) }
            } else {
                filteredRoster.forEach { r ->
                    ReadOnlyRosterRow(r)
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(80.dp))
        }
        TextCta("Export CSV", onClick = { exportCsv(context, session, roster) }, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(4.dp))
        Text(
            session?.let { "Session closed at ${formatTime(it.endAt)}." } ?: "",
            color = TextMuted, fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ReadOnlyRosterRow(rec: AttendanceRecord) {
    WhiteCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(AccentPrimary),
                contentAlignment = Alignment.Center
            ) { Text(rec.userId.firstOrNull()?.uppercase() ?: "?", color = SurfaceWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rec.userId.take(8), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                val line = when {
                    rec.selfMarkAt != null -> "Marked at ${formatTime(rec.selfMarkAt)}"
                    else -> "Not marked"
                }
                Text(line, color = TextMuted, fontSize = 12.sp)
            }
            val dot: Color = when (rec.finalFlag) {
                "Present" -> Success
                "Absent", "PresentButNotAtLecture" -> ErrorRed
                "PresentButLeftInBetween" -> Warning
                else -> TextMuted
            }
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dot))
        }
    }
}

private fun exportCsv(context: android.content.Context, session: Session?, roster: List<AttendanceRecord>) {
    val sb = StringBuilder("user_id,self_mark_at,ble_first,ble_last,final_flag,override\n")
    roster.forEach { r ->
        sb.appendLine(listOf(r.userId, r.selfMarkAt ?: "", r.bleFirstDetectedAt ?: "", r.bleLastDetectedAt ?: "", r.finalFlag ?: "", r.instructorOverride ?: "").joinToString(","))
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        putExtra(Intent.EXTRA_SUBJECT, "Attendance ${session?.classroom ?: ""}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Export CSV").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
private fun Modifier.horizontalScrollEnable3(): Modifier {
    val s = androidx.compose.foundation.rememberScrollState()
    return this.then(Modifier.horizontalScroll(s))
}
