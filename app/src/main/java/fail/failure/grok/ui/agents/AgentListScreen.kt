package fail.failure.grok.ui.agents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fail.failure.grok.ui.components.AgentCard
import fail.failure.grok.ui.components.AgentCardSkeleton
import fail.failure.grok.ui.components.AmbientBackground
import fail.failure.grok.ui.components.ApiKeyRequiredCard
import fail.failure.grok.ui.components.StaggeredItem
import fail.failure.grok.ui.theme.GrokAccent
import fail.failure.grok.ui.theme.GrokTextFieldShape
import fail.failure.grok.ui.theme.GrokTextSecondary
import fail.failure.grok.ui.theme.cursorFilledTextFieldColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentListScreen(
    viewModel: AgentsViewModel,
    onOpenAgent: (String) -> Unit,
    onNewAgent: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    AmbientBackground(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Chats") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Filled.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        AgentSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.label) },
                                onClick = {
                                    showSortMenu = false
                                    viewModel.updateSortOrder(order)
                                },
                                trailingIcon = {
                                    if (state.sortOrder == order) {
                                        Text("✓", color = GrokAccent)
                                    }
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewAgent,
                shape = CircleShape,
                containerColor = GrokAccent,
                contentColor = Color.Black,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New chat")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.agents.isNotEmpty() || state.searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    placeholder = { Text("Search chats") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = GrokTextFieldShape,
                    colors = cursorFilledTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.isLoading && state.agents.isEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(6) { AgentCardSkeleton() }
                        }
                    }
                    state.needsApiKey -> {
                        ApiKeyRequiredCard(
                            onSubmit = viewModel::signInWithApiKey,
                            isSubmitting = state.isLoading || state.isRefreshing,
                            errorMessage = state.apiKeyError,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    state.agents.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("🤖", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.padding(top = 8.dp))
                                Text(
                                    "No chats yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    "Tap + to start a chat with Grok Build.",
                                    color = GrokTextSecondary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    state.filteredAgents.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                "No chats match your search.",
                                color = GrokTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            itemsIndexed(state.filteredAgents, key = { _, agent -> agent.id }) { index, agent ->
                                StaggeredItem(index = index) {
                                    AgentCard(
                                        agent = agent,
                                        onClick = { onOpenAgent(agent.id) },
                                        pinned = agent.id in state.pinnedIds,
                                        onLongClick = { viewModel.togglePin(agent.id) },
                                        onArchive = { viewModel.archive(agent.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
