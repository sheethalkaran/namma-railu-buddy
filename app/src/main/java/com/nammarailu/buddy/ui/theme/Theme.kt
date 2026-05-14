package com.nammarailu.buddy.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Brand Colors ──────────────────────────────────────────────────────────────
object RailuColors {
    val DeepBlue      = Color(0xFF0B1F3A)
    val DeepBlueMid   = Color(0xFF102645)
    val Purple        = Color(0xFF5B2EFF)
    val PurpleLight   = Color(0xFF7B54FF)
    val OnTime        = Color(0xFF22C55E)
    val Delayed       = Color(0xFFEF4444)
    val Warning       = Color(0xFFF59E0B)
    val CardDark      = Color(0xFF152B4A)
    val SurfaceDark   = Color(0xFF0F2035)
    val TextPrimary   = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB0C4DE)
    val DividerColor  = Color(0xFF1E3A5F)
    val Gold          = Color(0xFFFFD700)
    val CoachEngine   = Color(0xFF374151)
    val CoachGen      = Color(0xFF6B7280)
    val CoachLadies   = Color(0xFFEC4899)
    val CoachSleeper  = Color(0xFF3B82F6)
    val CoachAC3      = Color(0xFF8B5CF6)
    val CoachAC2      = Color(0xFF06B6D4)
    val CoachPantry   = Color(0xFFF97316)
}

// ── Dark Color Scheme ─────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = RailuColors.Purple,
    onPrimary        = Color.White,
    primaryContainer = RailuColors.DeepBlueMid,
    secondary        = RailuColors.PurpleLight,
    background       = RailuColors.DeepBlue,
    surface          = RailuColors.SurfaceDark,
    onBackground     = RailuColors.TextPrimary,
    onSurface        = RailuColors.TextPrimary,
    outline          = RailuColors.DividerColor,
    error            = RailuColors.Delayed
)

// ── Light Color Scheme ────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = RailuColors.Purple,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFEDE9FF),
    secondary        = RailuColors.PurpleLight,
    background       = Color(0xFFF1F5F9), // Lighter slate
    surface          = Color.White,
    onBackground     = Color(0xFF0F172A), // Slate 900
    onSurface        = Color(0xFF0F172A), // Slate 900
    outline          = Color(0xFFE2E8F0),
    error            = RailuColors.Delayed
)

// ── Typography ────────────────────────────────────────────────────────────────
val RailuTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 32.sp, lineHeight = 40.sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 28.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 24.sp, lineHeight = 32.sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.SemiBold,fontSize = 20.sp,lineHeight = 28.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.SemiBold,fontSize = 18.sp,lineHeight = 24.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)

// ── Shapes ────────────────────────────────────────────────────────────────────
val RailuShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small      = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium     = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large      = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

// ── Theme Composable ──────────────────────────────────────────────────────────
@Composable
fun NammaRailuTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = RailuTypography,
        shapes      = RailuShapes,
        content     = content
    )
}
