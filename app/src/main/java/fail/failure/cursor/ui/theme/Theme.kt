package fail.failure.cursor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Cursor's own app leans on a near-black surface with a single blue accent and system-style
// (SF Pro on iOS) typography - this reproduces that rather than a stock Material look.
val CursorBackground = Color(0xFF0B0B0D)
val CursorSurface = Color(0xFF18181B)
val CursorSurfaceRaised = Color(0xFF222226)
val CursorAccent = Color(0xFF4C8DFF)
val CursorTextPrimary = Color(0xFFF5F5F7)
val CursorTextSecondary = Color(0xFF9A9AA2)
val CursorBorder = Color(0xFF2A2A2E)
val CursorSuccess = Color(0xFF3DD68C)
val CursorWarning = Color(0xFFF5A623)
val CursorError = Color(0xFFFF6B6B)

private val CursorColorScheme = darkColorScheme(
    primary = CursorAccent,
    onPrimary = Color.White,
    background = CursorBackground,
    onBackground = CursorTextPrimary,
    surface = CursorSurface,
    onSurface = CursorTextPrimary,
    surfaceVariant = CursorSurfaceRaised,
    onSurfaceVariant = CursorTextSecondary,
    outline = CursorBorder,
    error = CursorError,
)

private val CursorTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

@Composable
fun CursorAndroidTheme(content: @Composable () -> Unit) {
    // Cursor's app doesn't offer a light theme worth emulating; always dark, like the IDE.
    val isDark = isSystemInDarkTheme() || true
    MaterialTheme(
        colorScheme = CursorColorScheme,
        typography = CursorTypography,
        content = content,
    )
}
