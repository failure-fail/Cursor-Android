package fail.failure.grok.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.delay

/** Each row in a freshly-loaded list rises and fades in a beat after the one before it, rather
 * than the whole list just appearing at once - a small cue that this is a living list, not a
 * static table. Only worth the per-index delay for the first composition of a short, glanceable
 * list (an agents feed), not for anything long or frequently recomposed. */
@Composable
fun StaggeredItem(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(16f) }

    LaunchedEffect(Unit) {
        delay((index * 45L).coerceAtMost(400L))
        alpha.animateTo(1f, tween(280))
    }
    LaunchedEffect(Unit) {
        delay((index * 45L).coerceAtMost(400L))
        offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .alpha(alpha.value)
            .offset(y = offsetY.value.dp),
    ) {
        content()
    }
}
