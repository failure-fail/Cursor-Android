package fail.failure.grok.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import fail.failure.grok.ui.theme.GrokAccent
import fail.failure.grok.ui.theme.GrokSurfaceRaised
import fail.failure.grok.ui.theme.GrokTextSecondary

data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)

/**
 * A floating pill-shaped tab bar rather than an edge-to-edge Material [androidx.compose.material3.NavigationBar] -
 * consistent with the rest of the app's rounded, floating surfaces (pill buttons, pill input bar)
 * instead of a flat full-width strip.
 */
@Composable
fun GrokBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(GrokSurfaceRaised),
    ) {
        items.forEach { item ->
            val selected = item.route == currentRoute
            val tint by animateColorAsState(
                targetValue = if (selected) GrokAccent else GrokTextSecondary,
                label = "bottomBarTint",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(item.route) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(item.icon, contentDescription = item.label, tint = tint)
                Text(item.label, color = tint, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
