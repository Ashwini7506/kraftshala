package ai.kraftshala.attendance.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ai.kraftshala.attendance.ui.common.KsScreen
import ai.kraftshala.attendance.ui.common.PrimaryCta
import ai.kraftshala.attendance.ui.common.WhiteCard
import ai.kraftshala.attendance.ui.theme.AccentPrimary
import ai.kraftshala.attendance.ui.theme.Success
import ai.kraftshala.attendance.ui.theme.SurfaceWhite
import ai.kraftshala.attendance.ui.theme.TextMuted
import ai.kraftshala.attendance.ui.theme.TextPrimary

private data class PermGroup(
    val key: String,
    val icon: ImageVector,
    val title: String,
    val helper: String,
    val permissions: Array<String>
)

@Composable
fun PermissionsScreen(onContinue: () -> Unit) {
    val context = LocalContext.current

    val groups = remember {
        listOf(
            PermGroup(
                "bluetooth", Icons.Default.Bluetooth, "Bluetooth", "We use it to confirm you're in the room.",
                if (Build.VERSION.SDK_INT >= 31)
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
                else arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
            ),
            PermGroup(
                "location", Icons.Default.LocationOn, "Location", "Only checked during session windows.",
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            ),
            PermGroup(
                "notifications", Icons.Default.Notifications, "Notifications", "So you don't miss a session reminder.",
                if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else arrayOf()
            )
        )
    }

    val granted = remember { mutableStateMapOf<String, Boolean>() }

    fun isGranted(group: PermGroup): Boolean {
        if (group.permissions.isEmpty()) return true
        return group.permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    LaunchedEffect(Unit) {
        groups.forEach { granted[it.key] = isGranted(it) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        groups.forEach { granted[it.key] = isGranted(it) }
    }

    KsScreen {
        Spacer(Modifier.height(24.dp))
        Text("Three permissions,\nthat's it", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp)
        Spacer(Modifier.height(8.dp))
        Text("All needed for attendance to work. We never sell or share your data.", color = TextMuted, fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))

        groups.forEach { g ->
            PermissionRow(
                group = g,
                granted = granted[g.key] == true,
                onGrant = {
                    if (g.permissions.isNotEmpty()) launcher.launch(g.permissions) else granted[g.key] = true
                }
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.weight(1f))

        val allGranted = groups.all { granted[it.key] == true }
        PrimaryCta("Continue", enabled = allGranted, onClick = onContinue)
    }
}

@Composable
private fun PermissionRow(group: PermGroup, granted: Boolean, onGrant: () -> Unit) {
    WhiteCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AccentPrimary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(group.icon, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(group.helper, fontSize = 13.sp, color = TextMuted)
            }
            if (granted) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Success),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Granted", tint = SurfaceWhite, modifier = Modifier.size(20.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(AccentPrimary)
                        .clickable { onGrant() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Grant", color = SurfaceWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}
