package ai.kraftshala.attendance.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.kraftshala.attendance.auth.AuthViewModel
import ai.kraftshala.attendance.ui.theme.BgForm
import ai.kraftshala.attendance.ui.theme.TextMuted

@Composable
fun ProfileScreen(
    role: String,
    onHome: () -> Unit,
    onSessions: () -> Unit,
    onSignedOut: () -> Unit
) {
    val authVm: AuthViewModel = viewModel()
    val authState by authVm.state.collectAsState()
    val user = authState.user

    Column(modifier = Modifier.fillMaxSize().background(BgForm).statusBarsPadding()) {
        Column(modifier = Modifier.weight(1f).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("Profile", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                InitialAvatar(user?.fullName ?: "?", size = 88)
            }
            Spacer(Modifier.height(16.dp))
            Text(user?.fullName ?: "—", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
            Text(user?.email ?: "—", color = TextMuted, fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
            Text("Role: ${user?.role ?: role}", color = TextMuted, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
            Spacer(Modifier.height(32.dp))
            PrimaryCta("Sign out", onClick = {
                authVm.signOut { onSignedOut() }
            })
        }
        BottomTabBar(active = "profile", onHome = onHome, onSessions = onSessions, onProfile = {})
    }
}
