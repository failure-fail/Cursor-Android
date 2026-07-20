package fail.failure.cursor.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import fail.failure.cursor.auth.AuthRepository
import fail.failure.cursor.ui.components.AmbientBackground
import fail.failure.cursor.ui.theme.CursorAccent
import fail.failure.cursor.ui.theme.CursorError
import fail.failure.cursor.ui.theme.CursorSurface
import fail.failure.cursor.ui.theme.CursorTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(authRepository: AuthRepository, onSignedOut: () -> Unit) {
    val session by authRepository.session.collectAsState()
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) {
            null
        }
    }

    val signInMethod = if (session.apiKey != null) "Personal API key" else "Cursor account"
    val identity = session.userId ?: session.apiKey?.take(10)?.plus("…") ?: "Signed in"
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CursorSurface, RoundedCornerShape(18.dp))
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Brush.linearGradient(listOf(CursorAccent, Color(0xFFE89A78))),
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
                        color = CursorTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CursorSurface, RoundedCornerShape(18.dp)),
            ) {
                SettingsRow(label = "Credential type", value = signInMethod)
                session.userId?.let { SettingsRow(label = "Account ID", value = it) }
                session.apiKey?.let { SettingsRow(label = "API key", value = "${it.take(10)}••••••") }
            }

            Button(
                onClick = {
                    authRepository.signOut()
                    onSignedOut()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CursorError, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text("Sign out")
            }

            versionName?.let {
                Text(
                    "Cursor for Android · v$it",
                    color = CursorTextSecondary,
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
        Text(label, color = CursorTextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
