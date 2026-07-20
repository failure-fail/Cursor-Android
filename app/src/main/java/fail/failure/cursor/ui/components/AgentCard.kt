package fail.failure.cursor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fail.failure.cursor.network.model.Agent
import fail.failure.cursor.network.model.EnvInput
import fail.failure.cursor.ui.theme.CursorSurface
import fail.failure.cursor.ui.theme.CursorTextSecondary

/** Mirrors the "All Repos → Recents" row style from Cursor's own mobile UI: a small status dot,
 * a bold title, and a "status · repo" secondary line - rather than a boxy title+badge layout. */
@Composable
fun AgentCard(agent: Agent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CursorSurface, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
            val repoLabel = repo?.repositoryUrl ?: repo?.let { "${it.owner}/${it.repo}" } ?: "No repository"
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
