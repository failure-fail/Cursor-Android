package fail.failure.cursor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import fail.failure.cursor.ui.theme.CursorError
import fail.failure.cursor.ui.theme.CursorSurface
import fail.failure.cursor.ui.theme.CursorTextSecondary

/**
 * Shown when an api.cursor.com call comes back 401/403: the account session from the
 * browser-based login isn't a credential that endpoint recognizes, so agent features need a
 * personal API key instead. Lets the user get one and paste it in without leaving the current
 * screen.
 *
 * [isSubmitting] and [errorMessage] reflect the actual retry the parent screen kicks off after
 * "Save and retry" - without them, a rejected key looked identical to a successful one: the
 * screen briefly shows a spinner elsewhere, this card unmounts (its remembered text goes with
 * it), and if the retry fails it just remounts with a blank field and zero explanation, which
 * reads as "nothing happened" rather than "that key didn't work."
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CursorSurface, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Personal API key needed", style = MaterialTheme.typography.titleMedium)
        Text(
            "Your account is signed in, but agent features are a separate API that only accepts " +
                "a personal API key, not the account login.",
            style = MaterialTheme.typography.bodyMedium,
            color = CursorTextSecondary,
        )
        OutlinedButton(
            onClick = {
                CustomTabsIntent.Builder().build()
                    .launchUrl(context, "https://cursor.com/dashboard/api".toUri())
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Get an API key")
        }
        OutlinedTextField(
            value = apiKeyText,
            onValueChange = { apiKeyText = it },
            label = { Text("Paste API key") },
            placeholder = { Text("crsr_...") },
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
        )
        errorMessage?.let {
            Text(it, color = CursorError, style = MaterialTheme.typography.labelSmall)
        }
        Button(
            onClick = { onSubmit(apiKeyText) },
            enabled = apiKeyText.isNotBlank() && !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
            } else {
                Text("Save and retry")
            }
        }
    }
}
