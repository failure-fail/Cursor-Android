package fail.failure.grok.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fail.failure.grok.network.model.Agent
import fail.failure.grok.ui.theme.GrokTextSecondary

/** Chat row: status dot, title, and a "status · model" secondary line.
 * Rendered as a frosted [GlassCard]. Long-press toggles [pinned].
 * Archiving is a plain tap on the trailing icon. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AgentCard(
    agent: Agent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pinned: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "agentCardScale",
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentPadding = 16.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val isActive = agent.status?.lowercase() in setOf("running", "starting", "pending")
            PulsingDot(
                color = statusColor(agent.status),
                active = isActive,
                modifier = Modifier.padding(top = 6.dp, end = 10.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pinned) {
                        Text("📌 ", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = agent.name ?: agent.id,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val subtitle = listOfNotNull(
                    (agent.status ?: "unknown").replaceFirstChar { it.uppercase() },
                    agent.description?.trim()?.take(48)?.ifBlank { null },
                ).joinToString(" · ")
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrokTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onArchive != null) {
                IconButton(onClick = onArchive) {
                    Icon(Icons.Filled.Archive, contentDescription = "Archive", tint = GrokTextSecondary)
                }
            }
        }
    }
}
