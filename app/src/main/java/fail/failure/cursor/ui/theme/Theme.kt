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

// Cursor's real brand leans on a muted terracotta accent (verified against cursor.com/mobile's
// marketing screenshots), but this pass deliberately pushes every accent color more saturated and
// vivid on top of that base hue, per an explicit "super bold colors" request - a near-black
// surface still anchors it, but nothing on top of that surface is muted anymore.
val CursorBackground = Color(0xFF0B0B0D)
val CursorSurface = Color(0xFF18181B)
val CursorSurfaceRaised = Color(0xFF222226)
val CursorAccent = Color(0xFFFF6A3D)
val CursorAccentSecondary = Color(0xFF7B5CFF)
val CursorTextPrimary = Color(0xFFF5F5F7)
val CursorTextSecondary = Color(0xFF9A9AA2)
val CursorBorder = Color(0xFF2A2A2E)
val CursorSuccess = Color(0xFF00E676)
val CursorWarning = Color(0xFFFFC400)
val CursorError = Color(0xFFFF3B5C)
val CursorDiffAdded = Color(0xFF00E676)
val CursorDiffRemoved = Color(0xFFFF3B5C)

private val CursorColorScheme = darkColorScheme(
    primary = CursorAccent,
    onPrimary = Color.White,
    // Left unset, these fall back to Material's stock dark-theme indigo/purple - visible as an
    // out-of-place blue "All" filter chip, since FilterChip's selected state reads
    // secondaryContainer by default, not primary.
    secondary = CursorAccent,
    onSecondary = Color.White,
    secondaryContainer = CursorAccent,
    onSecondaryContainer = Color.White,
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
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp),
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
