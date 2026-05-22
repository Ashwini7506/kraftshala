package ai.kraftshala.attendance.ui.instructor

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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

@Composable
fun FacultyCockpitScreen(
    sessionId: String,
    onBack: () -> Unit,
    onEnded: () -> Unit,
    onBluetoothOff: () -> Unit
) {
    val vm: FacultyViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val btOn by rememberBluetoothEnabledState()

    var session by remember { mutableStateOf<Session?>(null) }
    LaunchedEffect(sessionId) {
        session = vm.loadSession(sessionId)
        vm.subscribeRoster(sessionId)
    }

    var activeFilter by remember { mutableStateOf("Present") }
    var selectedRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var showEndConfirm by remember { mutableStateOf(false) }

    val roster = state.roster
    val recordByUser = remember(roster) { roster.associateBy { it.userId } }
    // Build one entry per cohort student (Pair<displayName, AttendanceRecord?>)
    val entries = remember(state.cohortStudents, recordByUser) {
        state.cohortStudents.map { u -> u to recordByUser[u.id] }
    }

    fun isPresent(r: AttendanceRecord?) = r != null && (r.finalFlag == "Present" || (r.selfMarkAt != null && r.finalFlag == null && r.instructorOverride != "force_absent"))
    fun isAbsent(r: AttendanceRecord?)  = r == null || r.finalFlag == "Absent" || r.instructorOverride == "force_absent" || (r.selfMarkAt == null && r.finalFlag == null)
    fun isNotInRoom(r: AttendanceRecord?) = r?.finalFlag == "PresentButNotAtLecture"
    fun isLeftEarly(r: AttendanceRecord?) = r?.finalFlag == "PresentButLeftInBetween"

    val counts = mapOf(
        "Present" to entries.count { isPresent(it.second) },
        "Absent" to entries.count { isAbsent(it.second) && !isPresent(it.second) && !isNotInRoom(it.second) && !isLeftEarly(it.second) },
        "Not in room" to entries.count { isNotInRoom(it.second) },
        "Left early" to entries.count { isLeftEarly(it.second) }
    )
    val total = entries.size
    val present = counts["Present"] ?: 0
    val pct = if (total > 0) present * 100 / total else 0

    val filteredEntries = remember(activeFilter, entries) {
        when (activeFilter) {
            "Present"     -> entries.filter { isPresent(it.second) }
            "Absent"      -> entries.filter { isAbsent(it.second) && !isPresent(it.second) && !isNotInRoom(it.second) && !isLeftEarly(it.second) }
            "Not in room" -> entries.filter { isNotInRoom(it.second) }
            "Left early"  -> entries.filter { isLeftEarly(it.second) }
            else -> entries
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgForm).statusBarsPadding()) {
        if (!btOn) {
            Box(
                modifier = Modifier.fillMaxWidth().background(ErrorRed).padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bluetooth off — students can't mark in.", color = SurfaceWhite, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(
                        "Turn on", color = SurfaceWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        modifier = Modifier.clickable { onBluetoothOff() }
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(SurfaceWhite).clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.weight(1f))
                StatusPill("LIVE NOW", SuccessBg, SuccessText)
            }
            Spacer(Modifier.height(20.dp))
            Text("Class in ${session?.classroom ?: "—"}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(
                session?.let { "${it.classroom} • ${timeRange(it.startAt, it.endAt)}" } ?: "",
                color = TextMuted, fontSize = 14.sp
            )
            Spacer(Modifier.height(20.dp))

            WhiteCard {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$present / $total", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("Marked present", color = TextMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(InputFill)) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(pct / 100f).background(AccentPrimary))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().horizontalScrollEnable2()) {
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

            if (filteredEntries.isEmpty()) {
                WhiteCard { Text("No one in this bucket yet.", color = TextMuted, fontSize = 14.sp) }
            } else {
                filteredEntries.forEach { (user, rec) ->
                    RosterEntryRow(user.fullName, rec) {
                        // Build a synthetic record for unmarked students so override sheet works
                        val target = rec ?: AttendanceRecord(
                            id = "",
                            sessionId = sessionId,
                            userId = user.id,
                            selfMarkAt = null,
                            bleFirstDetectedAt = null,
                            bleLastDetectedAt = null,
                            bleDetectedThroughout = false,
                            instructorCalledAbsent = false,
                            instructorOverride = null,
                            finalFlag = null
                        )
                        selectedRecord = target
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            SecondaryCta("Override attendance", onClick = { /* opens bulk override; for now we use per-row sheet */ })
            Spacer(Modifier.height(8.dp))
            PrimaryCta("End session", onClick = { showEndConfirm = true })
        }
    }

    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            confirmButton = {
                Text("End session", color = ErrorRed, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        showEndConfirm = false
                        vm.endSession { onEnded() }
                    }.padding(8.dp)
                )
            },
            dismissButton = {
                Text("Cancel", color = TextMuted, modifier = Modifier.clickable { showEndConfirm = false }.padding(8.dp))
            },
            title = { Text("End this session?", fontWeight = FontWeight.Bold) },
            text = { Text("Attendance will be finalised and saved. This can't be undone.") }
        )
    }

    selectedRecord?.let { rec ->
        OverrideSheet(
            record = rec,
            displayName = state.nameMap[rec.userId] ?: rec.userId.take(12),
            onClose = { selectedRecord = null },
            onForcePresent = { vm.overrideRecord(rec.userId, "force_present"); selectedRecord = null },
            onForceAbsent = { reason -> vm.overrideRecord(rec.userId, "force_absent", reason); selectedRecord = null }
        )
    }
}

@Composable
private fun RosterEntryRow(displayName: String, rec: AttendanceRecord?, onClick: () -> Unit) {
    WhiteCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(AccentPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(displayName.firstOrNull()?.uppercase() ?: "?", color = SurfaceWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                val line = when {
                    rec?.instructorOverride == "force_present" -> "Marked present (override)"
                    rec?.instructorOverride == "force_absent" -> "Marked absent (override)"
                    rec?.selfMarkAt != null -> "Marked at ${formatTime(rec.selfMarkAt)}"
                    rec?.bleFirstDetectedAt != null -> "Phone not in room"
                    else -> "Not marked"
                }
                Text(line, color = TextMuted, fontSize = 12.sp)
            }
            val dotColor: Color = when {
                rec?.finalFlag == "Present" || (rec?.selfMarkAt != null && rec.instructorOverride != "force_absent") -> Success
                rec?.finalFlag == "PresentButLeftInBetween" -> Warning
                rec?.finalFlag == "Absent" || rec?.instructorOverride == "force_absent" -> ErrorRed
                rec?.finalFlag == "PresentButNotAtLecture" -> Warning
                else -> TextMuted
            }
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun OverrideSheet(
    record: AttendanceRecord,
    displayName: String,
    onClose: () -> Unit,
    onForcePresent: () -> Unit,
    onForceAbsent: (String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var reason by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState, containerColor = SurfaceWhite) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth()) {
            Text("Override attendance", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(displayName, color = TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            PrimaryCta("Force present", onClick = onForcePresent)
            Spacer(Modifier.height(12.dp))
            PillTextField(value = reason, onValueChange = { reason = it }, placeholder = "Reason for absent (optional)")
            Spacer(Modifier.height(12.dp))
            SecondaryCta("Force absent", onClick = { onForceAbsent(reason.ifBlank { null }) })
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Modifier.horizontalScrollEnable2(): Modifier {
    val s = androidx.compose.foundation.rememberScrollState()
    return this.then(Modifier.horizontalScroll(s))
}
