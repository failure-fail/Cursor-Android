package fail.failure.grok.ui.auth

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
import fail.failure.grok.ui.components.AmbientBackground
import fail.failure.grok.ui.components.GlowButton
import fail.failure.grok.ui.components.OrbLogo
import fail.failure.grok.ui.theme.GrokTextSecondary

private fun openInBrowser(context: android.content.Context, url: String, onFailed: () -> Unit) {
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
        return
    } catch (_: ActivityNotFoundException) {
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        onFailed()
    }
}

private fun copyToClipboard(context: android.content.Context, label: String, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
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
        val awaiting = state as? LoginUiState.AwaitingDevice ?: return@LaunchedEffect
        val url = awaiting.verificationUriComplete ?: awaiting.verificationUri
        openInBrowser(context, url) { viewModel.onBrowserLaunchFailed() }
        if (viewModel.uiState.value is LoginUiState.AwaitingDevice) {
            viewModel.onBrowserLaunched()
        }
    }

    AmbientBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OrbLogo()
            Spacer(modifier = Modifier.height(20.dp))
            Text("Grok", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Sign in with your xAI account the same way Grok Build does: " +
                    "a one-time code opens accounts.x.ai, and this app picks up " +
                    "the OAuth session once you approve.",
                style = MaterialTheme.typography.bodyMedium,
                color = GrokTextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))

            when (val current = state) {
                LoginUiState.SignedOut -> {
                    GlowButton(
                        text = "Continue with Grok Build",
                        onClick = { viewModel.beginAccountLogin() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is LoginUiState.AwaitingDevice -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Opening browser…", color = GrokTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(current.userCode, style = MaterialTheme.typography.headlineMedium)
                }
                is LoginUiState.Polling -> {
                    Text(current.userCode, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Enter this code at accounts.x.ai if prompted, then approve access.",
                        color = GrokTextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                openInBrowser(
                                    context,
                                    current.verificationUriComplete ?: current.verificationUri,
                                ) {}
                            },
                        ) { Text("Open again") }
                        TextButton(
                            onClick = {
                                copyToClipboard(context, "Grok user code", current.userCode)
                            },
                        ) { Text("Copy code") }
                    }
                    TextButton(onClick = { viewModel.retry() }) { Text("Cancel") }
                }
                LoginUiState.TimedOut -> {
                    Text("That took too long. Try again?", color = GrokTextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    GlowButton(
                        text = "Retry sign in",
                        onClick = { viewModel.beginAccountLogin() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is LoginUiState.Failed -> {
                    Text(current.message, color = GrokTextSecondary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    GlowButton(
                        text = "Retry sign in",
                        onClick = { viewModel.beginAccountLogin() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is LoginUiState.BrowserLaunchFailed -> {
                    Text(
                        "No browser could be opened. Open the link on any device and enter this code:",
                        color = GrokTextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(current.userCode, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            copyToClipboard(
                                context,
                                "Grok sign-in link",
                                current.verificationUriComplete ?: current.verificationUri,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Copy sign-in link") }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.onBrowserLaunched() }) {
                        Text("I opened it, keep waiting")
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
                    label = { Text("xAI API key") },
                    placeholder = { Text("xai-...") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.signInWithApiKey(apiKeyText) },
                    enabled = apiKeyText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sign in with API key")
                }
            }
        }
    }
}
