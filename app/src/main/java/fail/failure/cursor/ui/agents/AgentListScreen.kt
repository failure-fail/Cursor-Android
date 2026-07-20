package fail.failure.cursor.ui.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fail.failure.cursor.ui.components.AgentCard
import fail.failure.cursor.ui.components.AgentCardSkeleton
import fail.failure.cursor.ui.components.AmbientBackground
import fail.failure.cursor.ui.components.ApiKeyRequiredCard
import fail.failure.cursor.ui.components.StaggeredItem
import fail.failure.cursor.ui.theme.CursorError
import fail.failure.cursor.ui.theme.CursorTextFieldShape
import fail.failure.cursor.ui.theme.CursorTextSecondary
import fail.failure.cursor.ui.theme.cursorFilledTextFieldColors

private val statusFilters = listOf(
    null to "All",
    "running" to "Running",
    "finished" to "Finished",
    "error" to "Failed",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentListScreen(
    viewModel: AgentsViewModel,
    onOpenAgent: (String) -> Unit,
    onNewAgent: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    AmbientBackground(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Agents") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewAgent) {
                Icon(Icons.Filled.Add, contentDescription = "New agent")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.agents.isNotEmpty() || state.searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    placeholder = { Text("Search agents") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = CursorTextFieldShape,
                    colors = cursorFilledTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    items(statusFilters) { (value, label) ->
                        FilterChip(
                            selected = state.statusFilter == value,
                            onClick = { viewModel.updateStatusFilter(value) },
                            label = { Text(label) },
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(top = 4.dp))
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
                                    "No agents yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    "Tap + to launch one on a repo, just like Cursor on desktop.",
                                    color = CursorTextSecondary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    state.filteredAgents.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                "No agents match your search.",
                                color = CursorTextSecondary,
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
                                    SwipeToArchive(onArchive = { viewModel.archive(agent.id) }) {
                                        AgentCard(agent = agent, onClick = { onOpenAgent(agent.id) })
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
}

/** Swipe a row away (either direction) to archive it - a real full-swipe gesture rather than a
 * hidden menu item, with a torn-red archive icon revealed underneath as you drag. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToArchive(onArchive: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) onArchive()
            true
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CursorError.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.CenterStart
                },
            ) {
                Icon(Icons.Filled.Archive, contentDescription = "Archive", tint = Color.White)
            }
        },
    ) {
        content()
    }
}
