package ai.kraftshala.attendance.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.kraftshala.attendance.auth.AuthViewModel
import ai.kraftshala.attendance.ui.common.KsScreen
import ai.kraftshala.attendance.ui.common.PillTextField
import ai.kraftshala.attendance.ui.common.PrimaryCta
import ai.kraftshala.attendance.ui.common.TextCta
import ai.kraftshala.attendance.ui.common.WhiteCard
import ai.kraftshala.attendance.ui.theme.ErrorText
import ai.kraftshala.attendance.ui.theme.SuccessText
import ai.kraftshala.attendance.ui.theme.TextMuted

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, vm: AuthViewModel) {
    val state by vm.state.collectAsState()
    var email by remember { mutableStateOf("") }

    if (state.user != null) onLoggedIn()

    KsScreen {
        Spacer(Modifier.height(40.dp))
        Text("Sign in with your\nKraftshala email", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp)
        Spacer(Modifier.height(8.dp))
        Text("We'll send a magic link to your inbox.", color = TextMuted, fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))

        PillTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "you@kraftshala.com",
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(16.dp))

        if (state.magicLinkSent) {
            WhiteCard {
                Column {
                    Text("Check your inbox", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = SuccessText)
                    Spacer(Modifier.height(4.dp))
                    Text("Tap the link we emailed to $email to finish signing in.", color = TextMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    TextCta("Resend link", onClick = { vm.sendMagicLink(email) })
                }
            }
        } else {
            PrimaryCta(
                label = if (state.isLoading) "Sending…" else "Send sign-in link",
                enabled = email.contains("@") && !state.isLoading,
                onClick = { vm.sendMagicLink(email) }
            )
        }

        state.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = ErrorText, fontSize = 13.sp)
        }
    }
}
