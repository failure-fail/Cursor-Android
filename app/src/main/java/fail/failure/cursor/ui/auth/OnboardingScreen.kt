package fail.failure.cursor.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fail.failure.cursor.ui.theme.CursorTextSecondary

private data class OnboardingStep(val emoji: String, val title: String, val description: String)

private val steps = listOf(
    OnboardingStep(
        "🚀",
        "Launch agents from anywhere",
        "Kick off a Cursor background agent against any of your repos, right from your phone.",
    ),
    OnboardingStep(
        "💻",
        "Remote Control your desktop",
        "Hand a task to an agent running on your own machine and keep steering it on the go.",
    ),
    OnboardingStep(
        "🔔",
        "Stay in the loop",
        "Live status, notifications, and home-screen widgets so you know the moment an agent needs you.",
    ),
)

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Cursor", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(32.dp))
        steps.forEach { step ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 28.dp)) {
                Text(step.emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(step.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CursorTextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Get started")
        }
    }
}
