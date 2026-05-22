package ai.kraftshala.attendance.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.kraftshala.attendance.ui.common.KsScreen
import ai.kraftshala.attendance.ui.common.PrimaryCta
import ai.kraftshala.attendance.ui.common.WhiteCard
import ai.kraftshala.attendance.ui.theme.AccentPrimary
import ai.kraftshala.attendance.ui.theme.SurfaceWhite
import ai.kraftshala.attendance.ui.theme.TextMuted
import ai.kraftshala.attendance.ui.theme.TextPrimary

@Composable
fun RoleSelectionScreen(onContinue: (role: String) -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }

    KsScreen {
        Spacer(Modifier.height(24.dp))
        Text("Who are you joining as?", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(24.dp))

        RoleCard(
            icon = Icons.Default.Person,
            title = "Student",
            helper = "I'm here to learn.",
            selected = selected == "student"
        ) { selected = "student" }

        Spacer(Modifier.height(12.dp))

        RoleCard(
            icon = Icons.Default.School,
            title = "Faculty",
            helper = "I'm teaching this batch.",
            selected = selected == "instructor"
        ) { selected = "instructor" }

        Spacer(Modifier.weight(1f))
        PrimaryCta("Continue", enabled = selected != null, onClick = { selected?.let { onContinue(it) } })
    }
}

@Composable
private fun RoleCard(icon: ImageVector, title: String, helper: String, selected: Boolean, onClick: () -> Unit) {
    WhiteCard(selected = selected, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(40.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AccentPrimary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(helper, fontSize = 13.sp, color = TextMuted)
            }
        }
    }
}
