package ai.kraftshala.attendance.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.kraftshala.attendance.data.AttendanceRepository
import ai.kraftshala.attendance.domain.models.AttendanceRecord
import ai.kraftshala.attendance.domain.models.Session
import ai.kraftshala.attendance.ui.common.*
import ai.kraftshala.attendance.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun StudentSessionDetailScreen(sessionId: String, onBack: () -> Unit) {
    val vm: StudentViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { AttendanceRepository(context.applicationContext) }
    var session by remember { mutableStateOf<Session?>(null) }
    var record by remember { mutableStateOf<AttendanceRecord?>(null) }
    var rosterCount by remember { mutableStateOf(0) }
    var presentCount by remember { mutableStateOf(0) }

    LaunchedEffect(sessionId) {
        scope.launch {
            session = vm.loadSession(sessionId)
            record = vm.myRecord(sessionId)
            val roster = repo.rosterForSession(sessionId)
            rosterCount = roster.size
            presentCount = roster.count { it.finalFlag == "Present" }
        }
    }

    val s = session
    val r = record
    val isPresent = r?.finalFlag == "Present" || r?.finalFlag == "PresentButLeftInBetween"

    Column(modifier = Modifier.fillMaxSize().background(BgForm).statusBarsPadding().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(SurfaceWhite).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            StatusPill("CLOSED", InputFill, TextMuted)
        }
        Spacer(Modifier.height(20.dp))
        Text(s?.let { "Class in ${it.classroom}" } ?: "", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(
            s?.let { "${formatDateShort(it.startAt)} • ${timeRange(it.startAt, it.endAt)} • Room ${it.classroom}" } ?: "",
            color = TextMuted, fontSize = 13.sp
        )
        Spacer(Modifier.height(20.dp))

        WhiteCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(if (isPresent) Success else ErrorRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPresent) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null, tint = SurfaceWhite, modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(if (isPresent) "You were present" else "You were absent", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    if (r?.selfMarkAt != null) Text("Marked at ${formatTime(r.selfMarkAt)}", color = TextMuted, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        WhiteCard {
            Column {
                InfoLine("Instructor", s?.instructorId?.take(8) ?: "—")
                Spacer(Modifier.height(10.dp))
                InfoLine("Cohort", s?.cohortId?.take(8) ?: "—")
                Spacer(Modifier.height(10.dp))
                val pct = if (rosterCount > 0) (presentCount * 100 / rosterCount) else 0
                InfoLine("Class attendance", "$presentCount of $rosterCount ($pct%)")
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Verified at ${formatTime(r?.finalisedAtOrNull())} when the session closed.", color = TextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
    }
}

private fun AttendanceRecord.finalisedAtOrNull(): String? = this.bleLastDetectedAt ?: this.selfMarkAt

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextMuted, fontSize = 14.sp)
        Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
