package ai.kraftshala.attendance.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.kraftshala.attendance.domain.models.Session
import ai.kraftshala.attendance.ui.common.BottomTabBar
import ai.kraftshala.attendance.ui.common.InitialAvatar
import ai.kraftshala.attendance.ui.common.SectionLabel
import ai.kraftshala.attendance.ui.common.StatusPill
import ai.kraftshala.attendance.ui.common.WhiteCard
import ai.kraftshala.attendance.ui.common.formatTime
import ai.kraftshala.attendance.ui.common.timeRange
import ai.kraftshala.attendance.ui.common.todayLong
import ai.kraftshala.attendance.ui.theme.BgForm
import ai.kraftshala.attendance.ui.theme.InputFill
import ai.kraftshala.attendance.ui.theme.SuccessBg
import ai.kraftshala.attendance.ui.theme.SuccessText
import ai.kraftshala.attendance.ui.theme.TextMuted
import ai.kraftshala.attendance.ui.theme.TextPrimary

@Composable
fun StudentHomeScreen(
    onLecture: (Session) -> Unit,
    onSessions: () -> Unit,
    onProfile: () -> Unit
) {
    val vm: StudentViewModel = viewModel()
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgForm)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Hi, ${state.userName}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            InitialAvatar(state.userName, size = 40)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Text("Today", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text(todayLong(), color = TextMuted, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))

            SectionLabel("ONGOING")
            Spacer(Modifier.height(8.dp))

            val ongoing = state.today.firstOrNull { it.status == "active" }
            if (ongoing != null) {
                OngoingCard(ongoing) { onLecture(ongoing) }
            } else {
                WhiteCard { Text("Nothing live right now.", color = TextMuted, fontSize = 14.sp) }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("LATER TODAY")
            Spacer(Modifier.height(8.dp))

            val later = state.today.filter { it.status != "active" }
            if (later.isEmpty()) {
                WhiteCard { Text("Nothing else scheduled for today.", color = TextMuted, fontSize = 14.sp) }
            } else {
                later.forEach { s ->
                    LaterCard(s) { onLecture(s) }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        BottomTabBar("home", onHome = {}, onSessions = onSessions, onProfile = onProfile)
    }
}

@Composable
private fun OngoingCard(s: Session, onClick: () -> Unit) {
    WhiteCard(onClick = onClick) {
        Column {
            StatusPill("LIVE NOW", bg = SuccessBg, textColor = SuccessText)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Class in ${s.classroom}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(timeRange(s.startAt, s.endAt), color = TextMuted, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Room ${s.classroom}", color = TextMuted, fontSize = 13.sp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
            }
        }
    }
}

@Composable
private fun LaterCard(s: Session, onClick: () -> Unit) {
    WhiteCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(InputFill)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(formatTime(s.startAt), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Class in ${s.classroom}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Room ${s.classroom} • ${timeRange(s.startAt, s.endAt)}", color = TextMuted, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}
