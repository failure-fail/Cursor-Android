package fail.failure.cursor.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import fail.failure.cursor.CursorApp
import fail.failure.cursor.MainActivity
import fail.failure.cursor.network.model.Agent

private val StatusColors = mapOf(
    "running" to Color(0xFFF5A623),
    "finished" to Color(0xFF3DD68C),
    "completed" to Color(0xFF3DD68C),
    "error" to Color(0xFFFF6B6B),
    "failed" to Color(0xFFFF6B6B),
)

private val TextPrimary = ColorProvider(Color(0xFFF5F5F7))
private val TextSecondary = ColorProvider(Color(0xFF9A9AA2))
private val WidgetBg = Color(0xFF18181B)

class AgentStatusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as CursorApp
        val agents = try {
            if (app.authRepository.isSignedIn()) {
                app.apiClient.service.listAgents(limit = 5).agents
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }

        provideContent {
            WidgetContent(agents)
        }
    }

    @Composable
    private fun WidgetContent(agents: List<Agent>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetBg)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Text(
                "Cursor Agents",
                style = TextStyle(color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            if (agents.isEmpty()) {
                Text("No active agents", style = TextStyle(color = TextSecondary, fontSize = 12.sp))
            } else {
                agents.take(4).forEach { agent ->
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                    ) {
                        Text(
                            agent.name ?: agent.id,
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(color = TextPrimary, fontSize = 13.sp),
                            maxLines = 1,
                        )
                        Text(
                            agent.status ?: "?",
                            style = TextStyle(
                                color = ColorProvider(StatusColors[agent.status?.lowercase()] ?: Color(0xFF9A9AA2)),
                                fontSize = 12.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

class AgentStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgentStatusWidget()
}
