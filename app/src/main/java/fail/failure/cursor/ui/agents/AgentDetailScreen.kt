package fail.failure.cursor.ui.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import fail.failure.cursor.ui.components.GlassCard
import fail.failure.cursor.ui.components.MarkdownText
import fail.failure.cursor.ui.components.PillInputBar
import fail.failure.cursor.ui.components.StatusBadge
import fail.failure.cursor.ui.components.ThinkingIndicator
import fail.failure.cursor.ui.theme.CursorAccent
import fail.failure.cursor.ui.theme.CursorAccentSecondary
import fail.failure.cursor.ui.theme.CursorSurface
import fail.failure.cursor.ui.theme.CursorTextSecondary
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentDetailScreen(
    viewModel: AgentDetailViewModel,
    onBack: () -> Unit,
    onOpenUsage: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var followUp by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isRunning = state.runStatus?.lowercase() in setOf("running", "starting", "pending")
    val isArchived = state.agent?.archived == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.agent?.name ?: state.agent?.id ?: "Agent") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.runStatus?.let { StatusBadge(status = it, modifier = Modifier.padding(end = 4.dp)) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More actions")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (isRunning) {
                            DropdownMenuItem(
                                text = { Text("Cancel run") },
                                onClick = {
                                    showMenu = false
                                    viewModel.cancelRun()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Usage & artifacts") },
                            onClick = {
                                showMenu = false
                                onOpenUsage()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (isArchived) "Unarchive" else "Archive") },
                            onClick = {
                                showMenu = false
                                if (isArchived) viewModel.unarchive() else viewModel.archive()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            } else {
                state.gitInfo?.branches?.firstOrNull()?.let { gitBranch ->
                    GitInfoCard(prUrl = gitBranch.prUrl, branch = gitBranch.branch) { url ->
                        CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
                    }
                }

                if (state.transcript.isEmpty()) {
                    Text(
                        "No activity yet.",
                        color = CursorTextSecondary,
                        modifier = Modifier.padding(24.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.transcript) { line -> TranscriptLineView(line) }
                    }
                }

                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
                }

                PillInputBar(
                    value = followUp,
                    onValueChange = { followUp = it },
                    placeholder = "Follow up…",
                    onSend = {
                        if (followUp.isBlank() || state.isFollowUpSending) return@PillInputBar
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.sendFollowUp(followUp)
                        followUp = ""
                    },
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete agent?") },
            text = { Text("This permanently removes the agent and its data. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.delete(onDeleted = onBack)
                    },
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun GitInfoCard(prUrl: String?, branch: String?, onOpen: (String) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp), contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            branch?.let {
                Text("🌿 $it", style = MaterialTheme.typography.labelSmall, color = CursorTextSecondary)
            }
            prUrl?.let { url ->
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(listOf(CursorAccent, CursorAccentSecondary)),
                            RoundedCornerShape(50),
                        )
                        .clickable { onOpen(url) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("View PR", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun TranscriptLineView(line: TranscriptLine) {
    when (line) {
        is TranscriptLine.Assistant -> BubbleText(line.text, CursorSurface)
        is TranscriptLine.Thinking -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ThinkingIndicator()
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides CursorTextSecondary,
            ) {
                MarkdownText(line.text)
            }
        }
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
        MarkdownText(text)
    }
}
