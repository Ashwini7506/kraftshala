package ai.kraftshala.attendance.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.kraftshala.attendance.ui.theme.BgForm

/** Standard form-phase screen wrapper with 24dp horizontal margins on the off-white bg. */
@Composable
fun KsScreen(
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = Modifier
        .fillMaxSize()
        .background(BgForm)
        .statusBarsPadding()
        .padding(horizontal = 24.dp)
        .padding(top = 16.dp, bottom = 24.dp)
    if (scrollable) {
        Column(modifier = base.verticalScroll(rememberScrollState()), content = content)
    } else {
        Column(modifier = base, content = content)
    }
}
