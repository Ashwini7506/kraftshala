package ai.kraftshala.attendance.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font as GoogleFontFont
import androidx.compose.ui.unit.sp
import ai.kraftshala.attendance.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val plusJakartaGf = GoogleFont("Plus Jakarta Sans")
private val interGf = GoogleFont("Inter")

val PlusJakartaSans = FontFamily(
    GoogleFontFont(googleFont = plusJakartaGf, fontProvider = provider, weight = FontWeight.Normal),
    GoogleFontFont(googleFont = plusJakartaGf, fontProvider = provider, weight = FontWeight.Medium),
    GoogleFontFont(googleFont = plusJakartaGf, fontProvider = provider, weight = FontWeight.SemiBold),
    GoogleFontFont(googleFont = plusJakartaGf, fontProvider = provider, weight = FontWeight.Bold),
    GoogleFontFont(googleFont = plusJakartaGf, fontProvider = provider, weight = FontWeight.ExtraBold)
)

val Inter = FontFamily(
    GoogleFontFont(googleFont = interGf, fontProvider = provider, weight = FontWeight.Normal),
    GoogleFontFont(googleFont = interGf, fontProvider = provider, weight = FontWeight.Medium),
    GoogleFontFont(googleFont = interGf, fontProvider = provider, weight = FontWeight.SemiBold),
    GoogleFontFont(googleFont = interGf, fontProvider = provider, weight = FontWeight.Bold)
)

val KsTypography = Typography(
    displayLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = TextPrimary),
    displayMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = TextPrimary),
    headlineLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = TextPrimary),
    headlineMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = TextPrimary),
    headlineSmall = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary),
    titleLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TextPrimary),
    titleMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary),
    titleSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, color = TextPrimary),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = TextPrimary),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = TextMuted),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextPrimary)
)
