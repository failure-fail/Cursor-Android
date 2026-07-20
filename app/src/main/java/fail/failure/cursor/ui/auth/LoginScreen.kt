package fail.failure.cursor.ui.auth

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.browser.customtabs.CustomTabsIntent
import fail.failure.cursor.ui.theme.CursorTextSecondary

/** Opens [url] in a Custom Tab, falling back to a plain browser Intent, then giving up. */
private fun openInBrowser(context: android.content.Context, url: String, onFailed: () -> Unit) {
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
        return
    } catch (_: ActivityNotFoundException) {
        // fall through to plain browser intent
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        onFailed()
    }
}

private fun copyToClipboard(context: android.content.Context, url: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText("Cursor sign-in link", url))
}

@Composable
fun LoginScreen(viewModel: AuthViewModel, onSignedIn: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showApiKeyEntry by remember { mutableStateOf(false) }
    var apiKeyText by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is LoginUiState.SignedIn) onSignedIn()
    }

    LaunchedEffect(state) {
        val awaiting = state as? LoginUiState.AwaitingBrowser ?: return@LaunchedEffect
        openInBrowser(context, awaiting.url) { viewModel.onBrowserLaunchFailed(awaiting.url) }
        // Only transition to Polling if the launch above didn't already report failure.
        if (viewModel.uiState.value is LoginUiState.AwaitingBrowser) {
            viewModel.onBrowserLaunched(awaiting.url)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Cursor", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Sign in the same way you do on desktop: a browser tab opens" +
                " to your Cursor account login, and this app picks up the session" +
                " once you finish there.",
            style = MaterialTheme.typography.bodyMedium,
            color = CursorTextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))

        when (val current = state) {
            LoginUiState.SignedOut -> {
                Button(
                    onClick = { viewModel.beginAccountLogin() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Continue with Cursor account")
                }
            }
            is LoginUiState.AwaitingBrowser -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Opening browser…", color = CursorTextSecondary)
            }
            is LoginUiState.Polling -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Waiting for you to finish signing in…", color = CursorTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "If nothing opened, or you closed the tab by accident:",
                    style = MaterialTheme.typography.labelSmall,
                    color = CursorTextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { openInBrowser(context, current.url) {} }) {
                        Text("Open again")
                    }
                    TextButton(onClick = { copyToClipboard(context, current.url) }) {
                        Text("Copy link")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = { viewModel.retry() }) { Text("Cancel") }
            }
            LoginUiState.TimedOut -> {
                Text("That took too long. Try again?", color = CursorTextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.beginAccountLogin() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Retry sign in")
                }
            }
            is LoginUiState.BrowserLaunchFailed -> {
                Text(
                    "No browser app could be opened. Copy the link below and paste it into a browser.",
                    color = CursorTextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { copyToClipboard(context, current.url) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Copy sign-in link")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { viewModel.onBrowserLaunched(current.url) }) {
                    Text("I opened it manually, keep waiting")
                }
            }
            LoginUiState.SignedIn -> {
                CircularProgressIndicator()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { showApiKeyEntry = !showApiKeyEntry }) {
            Text("Use an API key instead")
        }

        if (showApiKeyEntry) {
            OutlinedTextField(
                value = apiKeyText,
                onValueChange = { apiKeyText = it },
                label = { Text("Cursor API key") },
                placeholder = { Text("crsr_...") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.signInWithApiKey(apiKeyText) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign in with API key")
            }
        }
    }
}
