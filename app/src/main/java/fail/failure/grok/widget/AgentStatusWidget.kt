package fail.failure.grok.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import fail.failure.grok.GrokApp
import fail.failure.grok.MainActivity
import fail.failure.grok.network.model.Agent
import java.time.Duration
import java.time.Instant

private val WidgetBg = Color(0xFF111111)
private val RowBg = Color(0xFF1A1A1A)
private val AccentColor = Color(0xFFF2F2F2)
private val TextPrimary = ColorProvider(Color(0xFFF5F5F5))
private val TextSecondary = ColorProvider(Color(0xFF9B9B9B))
private val AccentOnColor = ColorProvider(Color.Black)

private fun relativeTime(iso: String?): String? {
    val instant = iso?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    val minutes = Duration.between(instant, Instant.now()).toMinutes()
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        else -> "${minutes / (60 * 24)}d ago"
    }
}

class AgentStatusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as GrokApp
        val signedIn = app.authRepository.isSignedIn()
        val agents = try {
            if (signedIn) app.apiClient.service.listAgents(limit = 6).agents else emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        provideContent {
            WidgetContent(signedIn, agents)
        }
    }

    @Composable
    private fun WidgetContent(signedIn: Boolean, agents: List<Agent>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetBg)
                .cornerRadius(24.dp)
                .padding(14.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Box(
                    modifier = GlanceModifier.size(22.dp).background(AccentColor).cornerRadius(11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🤖", style = TextStyle(fontSize = 12.sp))
                }
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    "Chats",
                    style = TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .background(AccentColor)
                        .cornerRadius(14.dp)
                        .clickable(
                            actionStartActivity<MainActivity>(
                                actionParametersOf(WidgetNavigation.DESTINATION_KEY to WidgetNavigation.DESTINATION_NEW_AGENT),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", style = TextStyle(color = AccentOnColor, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                }
            }
            Spacer(modifier = GlanceModifier.height(10.dp))

            when {
                !signedIn -> EmptyState("Sign in to see your chats")
                agents.isEmpty() -> EmptyState("No chats yet — tap + to launch one")
                else -> {
                    agents.take(4).forEach { agent ->
                        AgentRow(agent)
                        Spacer(modifier = GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyState(message: String) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                message,
                style = TextStyle(color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center),
            )
        }
    }

    @Composable
    private fun AgentRow(agent: Agent) {
        val subtitle = listOfNotNull(
            agent.status?.replaceFirstChar { it.uppercase() },
            relativeTime(agent.updatedAt ?: agent.createdAt),
        ).joinToString(" · ")

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(RowBg)
                .cornerRadius(14.dp)
                .padding(10.dp)
                .clickable(
                    actionStartActivity<MainActivity>(
                        actionParametersOf(
                            WidgetNavigation.DESTINATION_KEY to WidgetNavigation.DESTINATION_OPEN_AGENT,
                            WidgetNavigation.AGENT_ID_KEY to agent.id,
                        ),
                    ),
                ),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Box(modifier = GlanceModifier.size(8.dp).background(AccentColor).cornerRadius(4.dp)) {}
            Spacer(modifier = GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    agent.name ?: agent.id,
                    style = TextStyle(color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                )
                Text(
                    subtitle,
                    style = TextStyle(color = TextSecondary, fontSize = 11.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

class AgentStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgentStatusWidget()
}
