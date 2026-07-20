package fail.failure.grok.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fail.failure.grok.ui.theme.GrokAccent
import fail.failure.grok.ui.theme.GrokSurfaceRaised
import fail.failure.grok.ui.theme.GrokTextSecondary

data class SegmentOption(val id: String, val label: String)

/**
 * A pill-shaped toggle where the selected option's background physically slides to the tapped
 * segment, replacing a row of separately-selectable [androidx.compose.material3.FilterChip]s -
 * one continuous control instead of two chips that happen to be mutually exclusive.
 */
@Composable
fun SegmentedToggle(
    options: List<SegmentOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(GrokSurfaceRaised, RoundedCornerShape(50)),
    ) {
        val segmentWidth = maxWidth / options.size
        val selectedIndex = options.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        val indicatorOffset by androidx.compose.animation.core.animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "segmentIndicator",
        )

        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .offset(x = indicatorOffset + 4.dp)
                .width(segmentWidth - 8.dp)
                .fillMaxHeight()
                .background(GrokAccent, RoundedCornerShape(50)),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            options.forEach { option ->
                val selected = option.id == selectedId
                val textColor by animateColorAsState(
                    targetValue = if (selected) Color.White else GrokTextSecondary,
                    label = "segmentTextColor",
                )
                Box(
                    modifier = Modifier
                        .width(segmentWidth)
                        .fillMaxSize()
                        .clickable { onSelect(option.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(option.label, color = textColor, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
