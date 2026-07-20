package fail.failure.cursor.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import fail.failure.cursor.ui.theme.CursorAccent

/**
 * Three dots that ripple in sequence, standing in for "the agent is composing a response" -
 * distinct from a generic circular spinner, closer to chat-app typing indicators.
 */
@Composable
fun ThinkingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thinking")
    Row(modifier = modifier) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, delayMillis = index * 150, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "thinkingDot$index",
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(alpha)
                    .background(CursorAccent, CircleShape),
            )
            if (index < 2) androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
        }
    }
}
