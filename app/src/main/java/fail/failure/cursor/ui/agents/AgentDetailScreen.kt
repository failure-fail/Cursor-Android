package fail.failure.cursor.ui.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fail.failure.cursor.ui.components.StatusBadge
import fail.failure.cursor.ui.theme.CursorSurface
import fail.failure.cursor.ui.theme.CursorTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentDetailScreen(viewModel: AgentDetailViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var followUp by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.agent?.name ?: state.agent?.id ?: "Agent") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { state.runStatus?.let { StatusBadge(status = it, modifier = Modifier.padding(end = 12.dp)) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.transcript) { line -> TranscriptLineView(line) }
                }

                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = followUp,
                        onValueChange = { followUp = it },
                        placeholder = { Text("Send a follow-up…") },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            viewModel.sendFollowUp(followUp)
                            followUp = ""
                        },
                        enabled = followUp.isNotBlank() && !state.isFollowUpSending,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun TranscriptLineView(line: TranscriptLine) {
    when (line) {
        is TranscriptLine.Assistant -> BubbleText(line.text, CursorSurface)
        is TranscriptLine.Thinking -> Text(
            line.text,
            color = CursorTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        is TranscriptLine.Tool -> Text(
            "🔧 ${line.name} — ${line.status}",
            color = CursorTextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
        is TranscriptLine.SystemNote -> Text(
            line.text,
            color = CursorTextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun BubbleText(text: String, background: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
