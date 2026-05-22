package ai.kraftshala.attendance.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import ai.kraftshala.attendance.domain.models.Session
import ai.kraftshala.attendance.ui.common.*
import ai.kraftshala.attendance.ui.theme.*
import android.widget.Toast

@Composable
fun LectureDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    onMarked: (String) -> Unit,
    onBluetoothOff: () -> Unit
) {
    val vm: StudentViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var session by remember { mutableStateOf<Session?>(null) }

    LaunchedEffect(sessionId) {
        session = vm.loadSession(sessionId)
    }

    val s = session
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgForm)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceWhite)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            if (s?.status == "active") {
                StatusPill("LIVE NOW", SuccessBg, SuccessText)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(s?.let { "Class in ${it.classroom}" } ?: "Loading…", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(s?.let { "Instructor • Room ${it.classroom}" } ?: "", color = TextMuted, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))

        WhiteCard {
            Column {
                InfoRow("Starts", formatTime(s?.startAt))
                Spacer(Modifier.height(10.dp))
                InfoRow("Ends", formatTime(s?.endAt))
                Spacer(Modifier.height(10.dp))
                InfoRow("Cohort", s?.cohortId?.take(8) ?: "—")
            }
        }
        Spacer(Modifier.height(28.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Ready when you are.", color = TextMuted, fontSize = 15.sp)
        }
        Spacer(Modifier.weight(1f))

        PrimaryCta(
            label = if (state.markedAt != null) "Marked" else "Mark me present",
            enabled = s != null && state.markedAt == null,
            onClick = {
                if (!isBluetoothOn(context)) { onBluetoothOff() }
                else vm.markPresent(sessionId) { ok ->
                    if (ok) onMarked(sessionId)
                    else {
                        val err = vm.state.value.lastError
                        val msg = if (err != null) "FAIL → $err" else "Couldn't confirm you're in the room. Try again in a moment."
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your phone confirms you're in the room.",
            color = TextMuted, fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextMuted, fontSize = 14.sp)
        Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
