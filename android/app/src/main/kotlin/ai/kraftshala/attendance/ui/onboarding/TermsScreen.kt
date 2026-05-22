package ai.kraftshala.attendance.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.kraftshala.attendance.ui.common.KsScreen
import ai.kraftshala.attendance.ui.common.PrimaryCta
import ai.kraftshala.attendance.ui.common.WhiteCard
import ai.kraftshala.attendance.ui.theme.AccentPrimary
import ai.kraftshala.attendance.ui.theme.InputFill
import ai.kraftshala.attendance.ui.theme.SurfaceWhite
import ai.kraftshala.attendance.ui.theme.TextMuted
import ai.kraftshala.attendance.ui.theme.TextPrimary

@Composable
fun TermsScreen(onAgree: () -> Unit) {
    var tos by remember { mutableStateOf(false) }
    var privacy by remember { mutableStateOf(false) }

    KsScreen {
        Spacer(Modifier.height(24.dp))
        Text("Before we begin,\nthe rules", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp)
        Spacer(Modifier.height(8.dp))
        Text("Quick read. Tap each link if you want the full version.", color = TextMuted, fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))

        CheckCard(
            checked = tos,
            onToggle = { tos = !tos },
            text = buildAnnotatedString {
                append("I accept the ")
                withStyle(SpanStyle(color = AccentPrimary, textDecoration = TextDecoration.Underline)) {
                    append("Terms of Service")
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        CheckCard(
            checked = privacy,
            onToggle = { privacy = !privacy },
            text = buildAnnotatedString {
                append("I accept the ")
                withStyle(SpanStyle(color = AccentPrimary, textDecoration = TextDecoration.Underline)) {
                    append("Privacy Policy")
                }
            }
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "By proceeding you let Kraftshala record your attendance signals during sessions only. No background tracking.",
            fontSize = 12.sp,
            color = TextMuted
        )

        Spacer(Modifier.weight(1f))
        PrimaryCta("I agree, continue", enabled = tos && privacy, onClick = onAgree)
    }
}

@Composable
private fun CheckCard(checked: Boolean, onToggle: () -> Unit, text: androidx.compose.ui.text.AnnotatedString) {
    WhiteCard(onClick = onToggle) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (checked) AccentPrimary else InputFill),
                contentAlignment = Alignment.Center
            ) {
                if (checked) Icon(Icons.Default.Check, contentDescription = null, tint = SurfaceWhite, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(text, fontSize = 15.sp, color = TextPrimary)
        }
    }
}
