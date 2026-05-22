package ai.kraftshala.attendance.ui.instructor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun FacultyHomeScreen(
    onStartSession: (Session) -> Unit,
    onOpenCockpit: (Session) -> Unit,
    onPrepare: (Session) -> Unit,
    onSessions: () -> Unit,
    onProfile: () -> Unit,
    onBluetoothOff: () -> Unit
) {
    val vm: FacultyViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(BgForm).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Hi, ${state.userName}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            InitialAvatar(state.userName, size = 40)
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Text("Today", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text(todayLong(), color = TextMuted, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))

            SectionLabel("STARTING NOW")
            Spacer(Modifier.height(8.dp))

            val starting = state.today.firstOrNull()
            if (starting != null) {
                StartingNowCard(
                    session = starting,
                    isActive = starting.status == "active",
                    onCta = {
                        if (!isBluetoothOn(context)) { onBluetoothOff(); return@StartingNowCard }
                        if (starting.status == "active") onOpenCockpit(starting)
                        else vm.startSession(starting) { updated -> onStartSession(updated) }
                    }
                )
            } else {
                WhiteCard { Text("Nothing scheduled right now.", color = TextMuted, fontSize = 14.sp) }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("LATER TODAY")
            Spacer(Modifier.height(8.dp))

            val later = if (state.today.size > 1) state.today.drop(1) else emptyList()
            if (later.isEmpty()) {
                WhiteCard { Text("Nothing else scheduled.", color = TextMuted, fontSize = 14.sp) }
            } else {
                later.forEach { s ->
                    LaterTeachCard(s) { onPrepare(s) }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        BottomTabBar("home", onHome = {}, onSessions = onSessions, onProfile = onProfile)
    }
}

@Composable
private fun StartingNowCard(session: Session, isActive: Boolean, onCta: () -> Unit) {
    WhiteCard {
        Column {
            StatusPill(if (isActive) "LIVE NOW" else "READY TO START", SuccessBg, SuccessText)
            Spacer(Modifier.height(12.dp))
            Text("Class in ${session.classroom}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("${timeRange(session.startAt, session.endAt)} • Room ${session.classroom}", color = TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text("Cohort ${session.cohortId.take(8)}", color = TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(14.dp))
            PrimaryCta(if (isActive) "Open cockpit" else "Start session", onClick = onCta)
        }
    }
}

@Composable
private fun LaterTeachCard(s: Session, onPrepare: () -> Unit) {
    WhiteCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(InputFill).padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(formatTime(s.startAt), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Class in ${s.classroom}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("${timeRange(s.startAt, s.endAt)} • Room ${s.classroom}", color = TextMuted, fontSize = 12.sp)
            }
            Text(
                "Prepare", color = AccentPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onPrepare() }.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}
