package fail.failure.cursor.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fail.failure.cursor.ui.theme.CursorSurface
import fail.failure.cursor.ui.theme.CursorSurfaceRaised

/** A sweeping highlight over a flat shape - the "content is on its way" placeholder used instead
 * of a bare spinner, so a loading list still reads as the shape of what's coming. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -400f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerTranslate",
    )
    val brush = Brush.linearGradient(
        colors = listOf(CursorSurface, CursorSurfaceRaised, CursorSurface),
        start = Offset(translate - 200f, 0f),
        end = Offset(translate + 200f, 0f),
    )
    androidx.compose.foundation.layout.Box(modifier = modifier.background(brush, shape))
}

/** Placeholder rows shaped like [AgentCard] - what the agent list shows while it's loading,
 * instead of a spinner floating in an otherwise blank screen. */
@Composable
fun AgentCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CursorSurface, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(end = 10.dp)) {
            ShimmerBox(modifier = Modifier.size(8.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBox(modifier = Modifier.width(140.dp).height(16.dp))
            ShimmerBox(modifier = Modifier.width(90.dp).height(12.dp))
        }
    }
}

private fun Modifier.size(size: Dp): Modifier = this.width(size).height(size)
