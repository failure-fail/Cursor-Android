package fail.failure.cursor.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fail.failure.cursor.auth.AuthRepository
import fail.failure.cursor.ui.theme.CursorTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(authRepository: AuthRepository, onBack: () -> Unit, onSignedOut: () -> Unit) {
    val session by authRepository.session.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (session.apiKey != null) "Signed in with API key" else "Signed in with Cursor account",
                style = MaterialTheme.typography.titleMedium,
            )
            session.userId?.let {
                Text("Account: $it", color = CursorTextSecondary)
            }
            Button(
                onClick = {
                    authRepository.signOut()
                    onSignedOut()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign out")
            }
        }
    }
}
