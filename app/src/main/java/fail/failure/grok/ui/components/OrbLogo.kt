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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import fail.failure.grok.ui.theme.GrokAccent
import fail.failure.grok.ui.theme.GrokAccentSecondary

/**
 * A slowly-rotating gradient ring around a solid core - this app's own mark, standing in for a
 * static app icon on the sign-in hero rather than reusing Grok's actual logo asset.
 */
@Composable
fun OrbLogo(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "orbLogo")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "orbLogoAngle",
    )

    Canvas(modifier = modifier.size(72.dp)) {
        val strokeWidth = 5.dp.toPx()
        rotate(angle) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        GrokAccent.copy(alpha = 0f),
                        GrokAccent,
                        GrokAccentSecondary,
                        GrokAccent.copy(alpha = 0f),
                    ),
                ),
                startAngle = 0f,
                sweepAngle = 300f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
            )
        }
        drawCircle(
            brush = Brush.radialGradient(listOf(GrokAccent, GrokAccentSecondary)),
            radius = size.minDimension / 2f - strokeWidth * 2.2f,
            center = Offset(size.width / 2f, size.height / 2f),
        )
    }
}
