package fail.failure.cursor.ui.agents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fail.failure.cursor.ui.components.AgentCard
import fail.failure.cursor.ui.theme.CursorTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentListScreen(
    viewModel: AgentsViewModel,
    onOpenAgent: (String) -> Unit,
    onNewAgent: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agents") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewAgent) {
                Icon(Icons.Filled.Add, contentDescription = "New agent")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.agents.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.agents.isEmpty() -> {
                    Text(
                        "No agents yet. Tap + to launch one on a repo, just like Cursor on desktop.",
                        color = CursorTextSecondary,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.agents, key = { it.id }) { agent ->
                            AgentCard(agent = agent, onClick = { onOpenAgent(agent.id) })
                        }
                    }
                }
            }
        }
    }
}
