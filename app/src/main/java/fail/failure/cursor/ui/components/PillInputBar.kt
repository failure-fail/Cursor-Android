package fail.failure.cursor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import fail.failure.cursor.ui.theme.CursorAccent
import fail.failure.cursor.ui.theme.CursorSurfaceRaised
import fail.failure.cursor.ui.theme.CursorTextPrimary
import fail.failure.cursor.ui.theme.CursorTextSecondary

/**
 * The rounded "+ ... send" input bar seen throughout Cursor's own mobile UI (new-agent prompt,
 * follow-up box) - a pill-shaped field with a circular leading action and a circular trailing
 * send button, rather than a boxy Material outlined field.
 */
@Composable
fun PillInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    onLeadingClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(CursorSurfaceRaised)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onLeadingClick != null) {
            PillIconButton(onClick = onLeadingClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add", tint = CursorTextPrimary)
            }
        } else {
            Box(modifier = Modifier.size(12.dp))
        }

        Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            if (value.isEmpty()) {
                Text(placeholder, color = CursorTextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = CursorTextPrimary),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(CursorAccent),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PillIconButton(onClick = onSend, tint = if (value.isNotBlank()) CursorAccent else CursorTextSecondary) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}

@Composable
private fun PillIconButton(
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = CursorTextPrimary,
    content: @Composable () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides tint) {
            content()
        }
    }
}
