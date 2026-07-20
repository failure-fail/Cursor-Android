package fail.failure.cursor.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import fail.failure.cursor.network.model.Agent
import fail.failure.cursor.network.model.EnvInput
import fail.failure.cursor.ui.theme.CursorTextSecondary

/** Mirrors the "All Repos → Recents" row style from Cursor's own mobile UI: a small status dot,
 * a bold title, and a "status · repo" secondary line - rather than a boxy title+badge layout.
 * Rendered as a frosted [GlassCard] rather than a flat surface fill. */
@Composable
fun AgentCard(agent: Agent, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
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
                Text(
                    text = agent.name ?: agent.id,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val repo = agent.repos?.firstOrNull()
                val repoLabel = repo?.url?.removePrefix("https://github.com/") ?: "No repository"
                Text(
                    text = "${(agent.status ?: "unknown").replaceFirstChar { it.uppercase() }} · $repoLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CursorTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (agent.env?.type == EnvInput.TYPE_MACHINE) {
                Text("💻", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
