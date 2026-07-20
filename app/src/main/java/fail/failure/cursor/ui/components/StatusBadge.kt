package fail.failure.cursor.ui.components

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
import fail.failure.cursor.ui.theme.CursorError
import fail.failure.cursor.ui.theme.CursorSuccess
import fail.failure.cursor.ui.theme.CursorTextSecondary
import fail.failure.cursor.ui.theme.CursorWarning

fun statusColor(status: String?): Color = when (status?.lowercase()) {
    "finished", "completed", "success" -> CursorSuccess
    "running", "starting", "pending" -> CursorWarning
    "error", "failed", "cancelled" -> CursorError
    else -> CursorTextSecondary
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
