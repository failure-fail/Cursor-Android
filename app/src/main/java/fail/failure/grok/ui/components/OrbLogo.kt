package fail.failure.grok.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/** Minimal Grok-style mark: rotating white glyph on black. */
@Composable
fun OrbLogo(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "orbLogo")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "orbLogoAngle",
    )

    Canvas(modifier = modifier.size(72.dp)) {
        val r = size.minDimension / 2f
        drawCircle(color = Color(0xFF1A1A1A), radius = r)
        rotate(angle) {
            val path = Path().apply {
                moveTo(center.x, center.y - r * 0.55f)
                cubicTo(
                    center.x + r * 0.55f, center.y - r * 0.15f,
                    center.x + r * 0.55f, center.y + r * 0.15f,
                    center.x, center.y + r * 0.55f,
                )
                cubicTo(
                    center.x - r * 0.55f, center.y + r * 0.15f,
                    center.x - r * 0.55f, center.y - r * 0.15f,
                    center.x, center.y - r * 0.55f,
                )
                close()
            }
            drawPath(path, color = Color.White.copy(alpha = 0.92f))
        }
        drawCircle(color = Color.Black, radius = r * 0.18f, center = Offset(center.x, center.y))
    }
}
