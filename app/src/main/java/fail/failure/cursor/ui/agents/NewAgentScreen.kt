package fail.failure.cursor.ui.agents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fail.failure.cursor.ui.theme.CursorTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAgentScreen(
    viewModel: NewAgentViewModel,
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.createdAgentId) {
        state.createdAgentId?.let { onCreated(it) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("New agent") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isLoadingOptions) {
                CircularProgressIndicator()
            } else {
                Text("Repository", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.repositories) { repo ->
                        FilterChip(
                            selected = state.selectedRepo == repo,
                            onClick = { viewModel.selectRepo(repo) },
                            label = { Text(repo.fullName ?: "${repo.owner}/${repo.repo}") },
                        )
                    }
                }

                Text("Model", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.models) { model ->
                        FilterChip(
                            selected = state.selectedModel == model,
                            onClick = { viewModel.selectModel(model) },
                            label = { Text(model.displayName ?: model.id) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.prompt,
                onValueChange = viewModel::updatePrompt,
                label = { Text("What should the agent do?") },
                modifier = Modifier.fillMaxWidth().height(160.dp),
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::submit,
                enabled = state.prompt.isNotBlank() && !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSubmitting) "Launching…" else "Launch agent")
            }
        }
    }
}
