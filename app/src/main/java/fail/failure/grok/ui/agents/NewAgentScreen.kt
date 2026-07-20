package fail.failure.grok.ui.agents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import fail.failure.grok.ui.components.ApiKeyRequiredCard
import fail.failure.grok.ui.components.GlowButton
import fail.failure.grok.ui.theme.GrokFilterChipShape
import fail.failure.grok.ui.theme.GrokTextFieldShape
import fail.failure.grok.ui.theme.cursorFilledTextFieldColors
import fail.failure.grok.ui.theme.cursorFilterChipColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAgentScreen(
    viewModel: NewAgentViewModel,
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(state.createdAgentId) {
        state.createdAgentId?.let { onCreated(it) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("New chat") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.needsApiKey) {
                item {
                    ApiKeyRequiredCard(
                        onSubmit = viewModel::signInWithApiKey,
                        isSubmitting = state.isLoadingOptions,
                        errorMessage = state.apiKeyError,
                    )
                }
            }

            item {
                Text("Model", style = MaterialTheme.typography.titleMedium)
                if (state.isLoadingOptions) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        items(state.models) { model ->
                            FilterChip(
                                selected = state.selectedModel == model,
                                onClick = { viewModel.selectModel(model) },
                                label = { Text(model.displayName ?: model.id) },
                                shape = GrokFilterChipShape,
                                colors = cursorFilterChipColors(),
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(promptTemplates) { template ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.updatePrompt(template.prompt) },
                                label = { Text(template.label) },
                                shape = GrokFilterChipShape,
                                colors = cursorFilterChipColors(),
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.prompt,
                        onValueChange = viewModel::updatePrompt,
                        label = { Text("Message Grok…") },
                        placeholder = { Text("Ask anything") },
                        shape = GrokTextFieldShape,
                        colors = cursorFilledTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    GlowButton(
                        text = "Start chat",
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.submit()
                        },
                        enabled = state.prompt.isNotBlank() && !state.isSubmitting,
                        loading = state.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private data class PromptTemplate(val label: String, val prompt: String)

private val promptTemplates = listOf(
    PromptTemplate("Explain", "Explain this clearly: "),
    PromptTemplate("Debug", "Help me debug: "),
    PromptTemplate("Rewrite", "Rewrite this more clearly: "),
    PromptTemplate("Brainstorm", "Brainstorm ideas for: "),
)
