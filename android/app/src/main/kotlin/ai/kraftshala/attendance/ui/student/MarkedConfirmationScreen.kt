package ai.kraftshala.attendance.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.kraftshala.attendance.domain.models.Session
import ai.kraftshala.attendance.ui.common.*
import ai.kraftshala.attendance.ui.theme.*

@Composable
fun MarkedConfirmationScreen(sessionId: String, onBackHome: () -> Unit) {
    val vm: StudentViewModel = viewModel()
    val state by vm.state.collectAsState()
    var session by remember { mutableStateOf<Session?>(null) }
    LaunchedEffect(sessionId) { session = vm.loadSession(sessionId) }

    KsScreen(scrollable = false) {
        Spacer(Modifier.height(48.dp))
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Success)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = SurfaceWhite, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("You're marked in", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(session?.let { "Class in ${it.classroom}" } ?: "", color = TextMuted, fontSize = 15.sp)
        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Marked at ${formatTime(state.markedAt)}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(24.dp))
        WhiteCard {
            Text("Keep your phone with you. We'll auto-check you're still here.", color = TextMuted, fontSize = 14.sp)
        }
        Spacer(Modifier.weight(1f))
        PrimaryCta("Back to today", onClick = onBackHome)
    }
}
