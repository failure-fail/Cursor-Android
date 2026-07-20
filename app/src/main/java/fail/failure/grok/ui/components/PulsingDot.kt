package fail.failure.grok.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A status dot that breathes outward with a soft trailing halo while [active], instead of a
 * flat static circle - the "this is live right now" cue used for running agents.
 */
@Composable
fun PulsingDot(color: Color, active: Boolean, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    if (!active) {
        Box(modifier = modifier.size(size).background(color, CircleShape))
        return
    }

    val transition = rememberInfiniteTransition(label = "pulsingDot")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(tween(1400), repeatMode = RepeatMode.Restart),
        label = "pulsingDotScale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1400), repeatMode = RepeatMode.Restart),
        label = "pulsingDotAlpha",
    )

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale)
                .background(color.copy(alpha = alpha), CircleShape),
        )
        Box(modifier = Modifier.size(size).background(color, CircleShape))
    }
}
