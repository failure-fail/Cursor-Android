package fail.failure.grok.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import fail.failure.grok.ui.theme.GrokError
import fail.failure.grok.ui.theme.GrokTextSecondary

/**
 * Shown when cli-chat-proxy returns 401/403 and an `xai-...` key may help
 * (parity with Grok Build's `XAI_API_KEY` fallback when no OAuth session works).
 */
@Composable
fun ApiKeyRequiredCard(
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
) {
    val context = LocalContext.current
    var apiKeyText by rememberSaveable { mutableStateOf("") }

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("xAI API key needed", style = MaterialTheme.typography.titleMedium)
            Text(
                "Grok Build usually uses your OAuth session. If the proxy still rejects " +
                    "requests, paste an API key from console.x.ai as a fallback.",
                style = MaterialTheme.typography.bodyMedium,
                color = GrokTextSecondary,
            )
            OutlinedButton(
                onClick = {
                    CustomTabsIntent.Builder().build()
                        .launchUrl(context, "https://console.x.ai".toUri())
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open console.x.ai")
            }
            OutlinedTextField(
                value = apiKeyText,
                onValueChange = { apiKeyText = it },
                label = { Text("Paste API key") },
                placeholder = { Text("xai-...") },
                isError = errorMessage != null,
                modifier = Modifier.fillMaxWidth(),
            )
            errorMessage?.let {
                Text(it, color = GrokError, style = MaterialTheme.typography.labelSmall)
            }
            Button(
                onClick = { onSubmit(apiKeyText) },
                enabled = apiKeyText.isNotBlank() && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(20.dp),
                    )
                } else {
                    Text("Save and retry")
                }
            }
        }
    }
}
