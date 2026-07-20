package fail.failure.grok.ui.components

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
import fail.failure.grok.ui.theme.GrokAccent
import fail.failure.grok.ui.theme.GrokSurfaceRaised
import fail.failure.grok.ui.theme.GrokTextPrimary
import fail.failure.grok.ui.theme.GrokTextSecondary

/**
 * The rounded "+ ... send" input bar seen throughout Grok's own mobile UI (new-agent prompt,
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
            .background(GrokSurfaceRaised)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onLeadingClick != null) {
            PillIconButton(onClick = onLeadingClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add", tint = GrokTextPrimary)
            }
        } else {
            Box(modifier = Modifier.size(12.dp))
        }

        Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            if (value.isEmpty()) {
                Text(placeholder, color = GrokTextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrokTextPrimary),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(GrokAccent),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PillIconButton(onClick = onSend, tint = if (value.isNotBlank()) GrokAccent else GrokTextSecondary) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}

@Composable
private fun PillIconButton(
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = GrokTextPrimary,
    content: @Composable () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides tint) {
            content()
        }
    }
}
