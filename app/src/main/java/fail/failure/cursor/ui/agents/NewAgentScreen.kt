package fail.failure.cursor.ui.agents

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import fail.failure.cursor.network.model.EnvInput
import fail.failure.cursor.network.model.ImageInput
import fail.failure.cursor.network.model.RepositoryInfo
import fail.failure.cursor.ui.theme.CursorTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAgentScreen(
    viewModel: NewAgentViewModel,
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showRepoPicker by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5),
    ) { uris: List<Uri> ->
        coroutineScope.launch(Dispatchers.IO) {
            uris.forEach { uri ->
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@forEach
                val mediaType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                viewModel.addImage(ImageInput(data = encoded, mediaType = mediaType))
            }
        }
    }

    LaunchedEffect(state.createdAgentId) {
        state.createdAgentId?.let { onCreated(it) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("New agent") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                if (state.isLoadingOptions) {
                    CircularProgressIndicator()
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Repository", style = MaterialTheme.typography.titleMedium)
                        RepoPickerButton(state.selectedRepo) { showRepoPicker = true }

                        Text("Run on", style = MaterialTheme.typography.titleMedium)
                        EnvTargetPicker(
                            target = state.envTarget,
                            keepAwake = state.keepMachineAwake,
                            onTargetChange = viewModel::selectEnvTarget,
                            onKeepAwakeChange = viewModel::setKeepMachineAwake,
                        )

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
                        state.selectedModel?.parameters?.forEach { param ->
                            val name = param.name
                            if (name != null) {
                                Text(name, style = MaterialTheme.typography.labelSmall, color = CursorTextSecondary)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(param.values.orEmpty()) { value ->
                                        FilterChip(
                                            selected = state.selectedModelParams[name] == value,
                                            onClick = { viewModel.selectModelParam(name, value) },
                                            label = { Text(value) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = viewModel::updatePrompt,
                    label = { Text("What should the agent do?") },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Images (${state.images.size}/5)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CursorTextSecondary,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            IconButton(
                                onClick = {
                                    imagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                enabled = state.images.size < 5,
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Add image")
                            }
                        }
                        items(state.images.size) { index ->
                            Box(modifier = Modifier.size(48.dp)) {
                                IconButton(onClick = { viewModel.removeImageAt(index) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove image ${index + 1}")
                                }
                            }
                        }
                    }
                }
            }

            item {
                TextButton(onClick = { viewModel.toggleAdvanced() }) {
                    Text(if (state.showAdvanced) "Hide advanced options" else "Advanced options (env vars, MCP servers)")
                }
            }

            if (state.showAdvanced) {
                item {
                    KeyValueEditor(
                        title = "Environment variables",
                        entries = state.envVars,
                        keyLabel = "Name",
                        valueLabel = "Value",
                        onAdd = viewModel::addEnvVar,
                        onUpdate = viewModel::updateEnvVar,
                        onRemove = viewModel::removeEnvVarAt,
                    )
                }
                item {
                    KeyValueEditor(
                        title = "MCP servers",
                        entries = state.mcpServers,
                        keyLabel = "Name",
                        valueLabel = "URL",
                        onAdd = viewModel::addMcpServer,
                        onUpdate = viewModel::updateMcpServer,
                        onRemove = viewModel::removeMcpServerAt,
                    )
                }
            }

            item {
                Column {
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.submit()
                        },
                        enabled = state.prompt.isNotBlank() && !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.isSubmitting) "Launching…" else "Launch agent")
                    }
                }
            }
        }
    }

    if (showRepoPicker) {
        RepoPickerSheet(
            repos = state.filteredRepositories,
            query = state.repoQuery,
            onQueryChange = viewModel::updateRepoQuery,
            onSelect = {
                viewModel.selectRepo(it)
                showRepoPicker = false
            },
            onDismiss = { showRepoPicker = false },
        )
    }
}

@Composable
private fun RepoPickerButton(selected: RepositoryInfo?, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(selected?.fullName ?: selected?.let { "${it.owner}/${it.repo}" } ?: "Choose a repository")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepoPickerSheet(
    repos: List<RepositoryInfo>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (RepositoryInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search repositories") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.height(400.dp)) {
                items(repos, key = { it.owner + "/" + it.repo }) { repo ->
                    Text(
                        repo.fullName ?: "${repo.owner}/${repo.repo}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(repo) }
                            .padding(vertical = 12.dp),
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EnvTargetPicker(
    target: String,
    keepAwake: Boolean,
    onTargetChange: (String) -> Unit,
    onKeepAwakeChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = target == EnvInput.TYPE_CLOUD,
                onClick = { onTargetChange(EnvInput.TYPE_CLOUD) },
                label = { Text("☁️ Cloud") },
            )
            FilterChip(
                selected = target == EnvInput.TYPE_MACHINE,
                onClick = { onTargetChange(EnvInput.TYPE_MACHINE) },
                label = { Text("💻 My machine (Remote Control)") },
            )
        }
        if (target == EnvInput.TYPE_MACHINE) {
            Column {
                Text(
                    "Requires Cursor desktop running and online on that machine.",
                    style = MaterialTheme.typography.labelSmall,
                    color = CursorTextSecondary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Keep computer awake", modifier = Modifier.padding(end = 8.dp))
                    Switch(checked = keepAwake, onCheckedChange = onKeepAwakeChange)
                }
            }
        }
    }
}

@Composable
private fun KeyValueEditor(
    title: String,
    entries: List<KeyValue>,
    keyLabel: String,
    valueLabel: String,
    onAdd: () -> Unit,
    onUpdate: (Int, String, String) -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        entries.forEachIndexed { index, entry ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = entry.key,
                    onValueChange = { onUpdate(index, it, entry.value) },
                    label = { Text(keyLabel) },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = entry.value,
                    onValueChange = { onUpdate(index, entry.key, it) },
                    label = { Text(valueLabel) },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove")
                }
            }
        }
        TextButton(onClick = onAdd) { Text("+ Add") }
    }
}
