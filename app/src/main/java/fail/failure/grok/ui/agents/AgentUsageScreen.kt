package fail.failure.grok.ui.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import fail.failure.grok.ui.theme.GrokSurface
import fail.failure.grok.ui.theme.GrokTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentUsageScreen(viewModel: AgentUsageViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.pendingDownloadUrl) {
        state.pendingDownloadUrl?.let { url ->
            CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
            viewModel.consumeDownloadUrl()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usage & artifacts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                val totals = state.usage?.totalUsage
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GrokSurface, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                ) {
                    Text("Token usage", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Input: ${totals?.inputTokens ?: 0}  ·  Output: ${totals?.outputTokens ?: 0}  ·  Total: ${totals?.totalTokens ?: 0}",
                        color = GrokTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                Text("Artifacts (${state.artifacts.size})", style = MaterialTheme.typography.titleMedium)
            }
            if (state.artifacts.isEmpty()) {
                item { Text("No artifacts produced yet.", color = GrokTextSecondary) }
            } else {
                items(state.artifacts, key = { it.path }) { artifact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrokSurface, RoundedCornerShape(10.dp))
                            .clickable { viewModel.downloadArtifact(artifact.path) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(artifact.path, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${(artifact.sizeBytes ?: 0) / 1024} KB",
                            color = GrokTextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            state.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        }
    }
}
