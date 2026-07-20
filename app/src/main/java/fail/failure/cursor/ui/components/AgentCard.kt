package fail.failure.cursor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fail.failure.cursor.network.model.Agent
import fail.failure.cursor.ui.theme.CursorSurface
import fail.failure.cursor.ui.theme.CursorTextSecondary

@Composable
fun AgentCard(agent: Agent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CursorSurface, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = agent.name ?: agent.id,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            StatusBadge(status = agent.status)
        }
        Spacer(modifier = Modifier.height(6.dp))
        val repo = agent.repos?.firstOrNull()
        Text(
            text = repo?.repositoryUrl ?: repo?.let { "${it.owner}/${it.repo}" } ?: "No repository",
            style = MaterialTheme.typography.bodyMedium,
            color = CursorTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
