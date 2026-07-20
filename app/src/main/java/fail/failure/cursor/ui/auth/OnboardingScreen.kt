package fail.failure.cursor.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fail.failure.cursor.ui.components.AmbientBackground
import fail.failure.cursor.ui.components.GlowButton
import fail.failure.cursor.ui.theme.CursorAccent
import fail.failure.cursor.ui.theme.CursorTextSecondary
import kotlinx.coroutines.launch

private data class OnboardingStep(val emoji: String, val title: String, val description: String)

private val steps = listOf(
    OnboardingStep(
        "🚀",
        "Launch agents from anywhere",
        "Kick off a Cursor background agent against any of your repos, right from your phone.",
    ),
    OnboardingStep(
        "💻",
        "Remote Control your desktop",
        "Hand a task to an agent running on your own machine and keep steering it on the go.",
    ),
    OnboardingStep(
        "🔔",
        "Stay in the loop",
        "Live status, notifications, and home-screen widgets so you know the moment an agent needs you.",
    ),
)

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == steps.lastIndex

    AmbientBackground(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Cursor", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.weight(1f))

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            val step = steps[page]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            ) {
                Text(step.emoji, style = MaterialTheme.typography.displaySmall)
                Spacer(modifier = Modifier.height(20.dp))
                Text(step.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    step.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CursorTextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(steps.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (selected) 10.dp else 8.dp)
                        .background(
                            if (selected) CursorAccent else CursorTextSecondary.copy(alpha = 0.35f),
                            CircleShape,
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        GlowButton(
            text = if (isLastPage) "Get started" else "Next",
            onClick = {
                if (isLastPage) {
                    onContinue()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    }
}
