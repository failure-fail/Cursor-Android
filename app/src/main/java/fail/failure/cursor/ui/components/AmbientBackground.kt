package fail.failure.cursor.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import fail.failure.cursor.ui.theme.CursorAccent
import fail.failure.cursor.ui.theme.CursorBackground
import kotlin.math.cos
import kotlin.math.sin

/**
 * Slow-drifting radial glow blobs over the near-black base, the ambient "ai-app" backdrop used
 * behind hero content (sign-in, empty states) instead of a flat surface color. Cheap to draw -
 * a handful of soft radial gradients on a Canvas, not a real blur pass - so it costs nothing on
 * low-end hardware while still reading as premium/atmospheric rather than a stock screen.
 */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val transition = rememberInfiniteTransition(label = "ambient")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "ambientPhase",
    )

    Box(modifier = modifier.fillMaxSize().background(CursorBackground)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            fun blob(cx: Float, cy: Float, radius: Float, color: Color) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0f)),
                        center = Offset(cx, cy),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(cx, cy),
                )
            }

            blob(
                cx = w * 0.25f + sin(t) * w * 0.08f,
                cy = h * 0.18f + cos(t * 0.8f) * h * 0.05f,
                radius = w * 0.65f,
                color = CursorAccent,
            )
            blob(
                cx = w * 0.85f + cos(t * 1.2f) * w * 0.06f,
                cy = h * 0.75f + sin(t * 0.7f) * h * 0.06f,
                radius = w * 0.55f,
                color = Color(0xFF6E6EF5),
            )
        }
        content()
    }
}
