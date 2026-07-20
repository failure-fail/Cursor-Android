package fail.failure.grok.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fail.failure.grok.ui.theme.GrokSurfaceRaised

/**
 * A frosted, faintly-lit glass panel instead of a flat solid card - a soft blurred tint layer
 * (real blur on API 31+, a plain translucent tint below that) under a thin light-edge border, so
 * content behind it (the ambient gradient, other cards) bleeds through softly rather than being
 * fully occluded by a flat surface color.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit = {},
) {
    Box(modifier = modifier.clip(shape)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.09f), GrokSurfaceRaised.copy(alpha = 0.62f)),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                )
                .let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) it.blur(22.dp) else it },
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.02f)),
                    ),
                    shape = shape,
                ),
        )
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
