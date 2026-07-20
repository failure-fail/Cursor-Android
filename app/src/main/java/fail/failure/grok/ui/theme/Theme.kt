package fail.failure.grok.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Grok Build visual language: near-black canvas, cool gray type, single white/silver accent.
// Deliberately not Cursor terracotta / purple.
val GrokBackground = Color(0xFF050505)
val GrokSurface = Color(0xFF111111)
val GrokSurfaceRaised = Color(0xFF1A1A1A)
val GrokAccent = Color(0xFFF2F2F2)
val GrokAccentSecondary = Color(0xFF8A8A8A)
val GrokTextPrimary = Color(0xFFF5F5F5)
val GrokTextSecondary = Color(0xFF9B9B9B)
val GrokBorder = Color(0xFF2A2A2A)
val GrokSuccess = Color(0xFF3DDC97)
val GrokWarning = Color(0xFFE8C547)
val GrokError = Color(0xFFFF5C5C)
val GrokDiffAdded = Color(0xFF3DDC97)
val GrokDiffRemoved = Color(0xFFFF5C5C)

private val GrokColorScheme = darkColorScheme(
    primary = GrokAccent,
    onPrimary = Color.Black,
    secondary = GrokAccentSecondary,
    onSecondary = Color.Black,
    secondaryContainer = GrokSurfaceRaised,
    onSecondaryContainer = GrokTextPrimary,
    background = GrokBackground,
    onBackground = GrokTextPrimary,
    surface = GrokSurface,
    onSurface = GrokTextPrimary,
    surfaceVariant = GrokSurfaceRaised,
    onSurfaceVariant = GrokTextSecondary,
    outline = GrokBorder,
    error = GrokError,
)

private val GrokTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

@Composable
fun GrokAndroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GrokColorScheme,
        typography = GrokTypography,
        content = content,
    )
}
