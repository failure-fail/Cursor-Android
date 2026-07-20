package fail.failure.grok.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fail.failure.grok.ui.theme.GrokError
import fail.failure.grok.ui.theme.GrokSuccess
import fail.failure.grok.ui.theme.GrokTextSecondary
import fail.failure.grok.ui.theme.GrokWarning

fun statusColor(status: String?): Color = when (status?.lowercase()) {
    "finished", "completed", "success" -> GrokSuccess
    "running", "starting", "pending" -> GrokWarning
    "error", "failed", "cancelled" -> GrokError
    else -> GrokTextSecondary
}

@Composable
fun StatusBadge(status: String?, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = (status ?: "unknown").replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
