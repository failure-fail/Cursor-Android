package fail.failure.cursor.ui.agents

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import fail.failure.cursor.network.model.Run
import fail.failure.cursor.ui.components.GlassCard
import fail.failure.cursor.ui.components.MarkdownText
import fail.failure.cursor.ui.components.PillInputBar
import fail.failure.cursor.ui.components.PulsingDot
import fail.failure.cursor.ui.components.StatusBadge
import fail.failure.cursor.ui.components.statusColor
import fail.failure.cursor.ui.theme.CursorAccent
import fail.failure.cursor.ui.theme.CursorAccentSecondary
import fail.failure.cursor.ui.theme.CursorSuccess
import fail.failure.cursor.ui.theme.CursorSurface
import fail.failure.cursor.ui.theme.CursorSurfaceRaised
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
    var showRunHistory by remember { mutableStateOf(false) }

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
                            text = { Text("Run history") },
                            onClick = {
                                showMenu = false
                                viewModel.loadRunHistory()
                                showRunHistory = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Copy agent ID") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Agent ID", state.agent?.id.orEmpty()))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                val shareUrl = state.gitInfo?.branches?.firstOrNull()?.prUrl
                                    ?: state.agent?.url
                                    ?: state.agent?.id.orEmpty()
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareUrl)
                                        },
                                        null,
                                    ),
                                )
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
                        items(groupTranscript(state.transcript)) { item -> TranscriptDisplayItemView(item) }
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

    if (showRunHistory) {
        RunHistorySheet(
            runs = state.runHistory,
            isLoading = state.isLoadingHistory,
            onDismiss = { showRunHistory = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RunHistorySheet(runs: List<Run>, isLoading: Boolean, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Run history", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.padding(bottom = 24.dp))
                runs.isEmpty() -> Text(
                    "No runs yet.",
                    color = CursorTextSecondary,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(runs, key = { it.id }) { run -> RunHistoryRow(run) }
                }
            }
        }
    }
}

@Composable
private fun RunHistoryRow(run: Run) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                StatusBadge(status = run.status)
                run.durationMs?.let {
                    Text(
                        "${it / 1000}s",
                        color = CursorTextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            run.createdAt?.let {
                Text(it, color = CursorTextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            run.result?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
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

/** What the transcript actually renders as, one step up from the raw [TranscriptLine] feed: runs
 * of thinking/tool-call activity are folded into a single collapsed live-progress row instead of
 * each one being its own permanently-visible line, with the full step-by-step detail underneath
 * only shown once expanded. Assistant replies and system notes stay as their own top-level items. */
private sealed interface TranscriptDisplayItem {
    data class Single(val line: TranscriptLine) : TranscriptDisplayItem
    data class ActivityGroup(val lines: List<TranscriptLine>) : TranscriptDisplayItem
}

private fun groupTranscript(transcript: List<TranscriptLine>): List<TranscriptDisplayItem> {
    val result = mutableListOf<TranscriptDisplayItem>()
    var buffer = mutableListOf<TranscriptLine>()
    fun flush() {
        if (buffer.isNotEmpty()) {
            result.add(TranscriptDisplayItem.ActivityGroup(buffer))
            buffer = mutableListOf()
        }
    }
    transcript.forEach { line ->
        when (line) {
            is TranscriptLine.Thinking, is TranscriptLine.Tool -> buffer.add(line)
            else -> {
                flush()
                result.add(TranscriptDisplayItem.Single(line))
            }
        }
    }
    flush()
    return result
}

@Composable
private fun TranscriptDisplayItemView(item: TranscriptDisplayItem) {
    when (item) {
        is TranscriptDisplayItem.Single -> TranscriptLineView(item.line)
        is TranscriptDisplayItem.ActivityGroup -> ActivityGroupView(item.lines)
    }
}

@Composable
private fun TranscriptLineView(line: TranscriptLine) {
    when (line) {
        is TranscriptLine.Assistant -> BubbleText(line.text, CursorSurface)
        is TranscriptLine.Thinking -> androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides CursorTextSecondary,
        ) {
            MarkdownText(line.text)
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

/** Collapsed by default, showing just the latest step as a live "progress message" (a pulsing dot
 * while it's still running, a checkmark once it isn't) - tap to expand the full run of thinking
 * and tool-call steps underneath it, in order. */
@Composable
private fun ActivityGroupView(lines: List<TranscriptLine>) {
    var expanded by remember { mutableStateOf(false) }
    val last = lines.last()
    val isRunning = when (last) {
        is TranscriptLine.Tool -> last.status.lowercase() in setOf("running", "pending", "started", "in_progress")
        is TranscriptLine.Thinking -> true
        else -> false
    }
    val summary = when (last) {
        is TranscriptLine.Thinking -> "💭 Thinking…"
        is TranscriptLine.Tool -> "🔧 ${last.name} — ${last.status}"
        else -> ""
    }
    val summaryColor = if (last is TranscriptLine.Tool) statusColor(last.status) else CursorTextSecondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CursorSurfaceRaised.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (isRunning) {
                PulsingDot(color = CursorAccent, active = true, size = 8.dp)
            } else {
                PulsingDot(color = CursorSuccess, active = false, size = 8.dp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                summary,
                color = summaryColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${lines.size} step${if (lines.size == 1) "" else "s"}",
                color = CursorTextSecondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(end = 4.dp),
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = CursorTextSecondary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                lines.forEach { line -> TranscriptLineView(line) }
            }
        }
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
