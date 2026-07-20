package fail.failure.grok.ui.auth

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
import fail.failure.grok.ui.components.AmbientBackground
import fail.failure.grok.ui.components.GlowButton
import fail.failure.grok.ui.theme.GrokAccent
import fail.failure.grok.ui.theme.GrokTextSecondary
import kotlinx.coroutines.launch

private data class OnboardingStep(val emoji: String, val title: String, val description: String)

private val steps = listOf(
    OnboardingStep(
        "💬",
        "Chat with Grok Build",
        "Start a conversation powered by the same cli-chat-proxy backend the Grok Build CLI uses.",
    ),
    OnboardingStep(
        "🔑",
        "Real Grok Build OAuth",
        "Sign in with auth.x.ai the same way the CLI does — device code + your xAI account.",
    ),
    OnboardingStep(
        "📱",
        "Built for your phone",
        "Chats stay on-device with live status and home-screen widgets when you need them.",
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
        Text("Grok", style = MaterialTheme.typography.titleLarge)
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
                    color = GrokTextSecondary,
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
                            if (selected) GrokAccent else GrokTextSecondary.copy(alpha = 0.35f),
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
