package fail.failure.grok.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fail.failure.grok.auth.AuthRepository
import fail.failure.grok.ui.components.AmbientBackground
import fail.failure.grok.ui.components.GlassCard
import fail.failure.grok.ui.theme.GrokAccent
import fail.failure.grok.ui.theme.GrokAccentSecondary
import fail.failure.grok.ui.theme.GrokError
import fail.failure.grok.ui.theme.GrokTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authRepository: AuthRepository,
    viewModel: SettingsViewModel,
    onSignedOut: () -> Unit,
) {
    val session by authRepository.session.collectAsState()
    val settingsState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) {
            null
        }
    }

    val me = settingsState.apiKeyInfo
    val signInMethod = if (session.apiKey != null) "xAI API key" else "Grok Build OAuth"
    val identity = me?.email ?: session.userId ?: session.apiKey?.take(10)?.plus("…") ?: "Signed in"
    val initial = identity.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: '?'

    AmbientBackground(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                Brush.linearGradient(listOf(GrokAccent, GrokAccentSecondary)),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(initial.toString(), color = Color.White, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(modifier = Modifier.padding(start = 14.dp))
                    Column {
                        Text(identity, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Signed in with $signInMethod",
                            color = GrokTextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                Column {
                    SettingsRow(label = "Credential type", value = signInMethod)
                    me?.email?.let { SettingsRow(label = "Email", value = it) }
                    me?.name?.let { SettingsRow(label = "Key name", value = it) }
                    session.userId?.let { SettingsRow(label = "Account ID", value = it) }
                    session.apiKey?.let { SettingsRow(label = "API key", value = "${it.take(10)}••••••") }
                }
            }

            Button(
                onClick = {
                    authRepository.signOut()
                    onSignedOut()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GrokError, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text("Sign out")
            }

            versionName?.let {
                Text(
                    "Grok for Android · v$it",
                    color = GrokTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = GrokTextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
