package ai.kraftshala.attendance.ui.common

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.kraftshala.attendance.ui.theme.SurfaceWhite
import ai.kraftshala.attendance.ui.theme.TextMuted
import ai.kraftshala.attendance.ui.theme.Warning

@Composable
fun BluetoothOffScreen(onAuto: () -> Unit, onLater: () -> Unit) {
    val context = LocalContext.current
    val btOn by rememberBluetoothEnabledState()

    LaunchedEffect(btOn) {
        if (btOn) onAuto()
    }

    KsScreen {
        Spacer(Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Warning)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = SurfaceWhite, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Bluetooth is off", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "We need it to confirm you're in the classroom. Turn it on and we'll continue automatically.",
            color = TextMuted,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(24.dp))
        WhiteCard {
            Column {
                Text("- We never scan when you're outside campus.", fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text("- Bluetooth stays on only during session windows.", fontSize = 14.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryCta("Open Bluetooth settings", onClick = {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        })
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextCta("I'll do it later", onClick = onLater)
        }
        Spacer(Modifier.height(8.dp))
    }
}
