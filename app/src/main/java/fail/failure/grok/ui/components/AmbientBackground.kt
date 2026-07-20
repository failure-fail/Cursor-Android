package fail.failure.grok.ui.components

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
import fail.failure.grok.ui.theme.GrokBackground
import kotlin.math.cos
import kotlin.math.sin

/** Soft monochrome atmosphere — no purple/terracotta glow blobs. */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val transition = rememberInfiniteTransition(label = "ambient")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Restart),
        label = "ambientPhase",
    )

    Box(modifier = modifier.fillMaxSize().background(GrokBackground)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            fun blob(cx: Float, cy: Float, radius: Float, alpha: Float) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = alpha), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(cx, cy),
                )
            }
            blob(
                cx = w * 0.2f + sin(t) * w * 0.05f,
                cy = h * 0.15f + cos(t * 0.7f) * h * 0.04f,
                radius = w * 0.7f,
                alpha = 0.06f,
            )
            blob(
                cx = w * 0.85f + cos(t) * w * 0.04f,
                cy = h * 0.8f + sin(t * 0.6f) * h * 0.05f,
                radius = w * 0.55f,
                alpha = 0.05f,
            )
        }
        content()
    }
}
