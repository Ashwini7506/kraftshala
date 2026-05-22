package ai.kraftshala.attendance.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.kraftshala.attendance.ui.theme.AccentPrimary
import ai.kraftshala.attendance.ui.theme.InputFill
import ai.kraftshala.attendance.ui.theme.SurfaceWhite
import ai.kraftshala.attendance.ui.theme.TextMuted
import ai.kraftshala.attendance.ui.theme.TextPrimary

/** Primary royal-blue 56dp pill CTA. */
@Composable
fun PrimaryCta(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val bg = if (enabled) AccentPrimary else AccentPrimary.copy(alpha = 0.30f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = SurfaceWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
    }
}

/** Secondary outline pill button (royal blue outline + label). */
@Composable
fun SecondaryCta(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(width = 1.5.dp, color = AccentPrimary, shape = RoundedCornerShape(28.dp))
            .background(SurfaceWhite)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = AccentPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
    }
}

/** Plain text button (royal blue label, no fill). */
@Composable
fun TextCta(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        label,
        color = AccentPrimary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    )
}

/** White surface card with 16dp radius and 20dp padding. */
@Composable
fun WhiteCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val mod = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(SurfaceWhite)
        .then(if (selected) Modifier.border(2.dp, AccentPrimary, RoundedCornerShape(16.dp)) else Modifier)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(20.dp)
    Box(modifier = mod) { content() }
}

/** Coloured status pill (e.g. "LIVE NOW", "CLOSED"). */
@Composable
fun StatusPill(label: String, bg: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Section label (small caps muted). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

/** Avatar circle with initial. */
@Composable
fun InitialAvatar(name: String, size: Int = 40) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(AccentPrimary),
        contentAlignment = Alignment.Center
    ) {
        Text(initial, color = SurfaceWhite, fontWeight = FontWeight.Bold, fontSize = (size * 0.40).sp)
    }
}

/** Pill input (read-only display style). For real input use PillTextField below. */
@Composable
fun PillContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(14.dp),
        color = InputFill
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 18.dp)) {
            content()
        }
    }
}
