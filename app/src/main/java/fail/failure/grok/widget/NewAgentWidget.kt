package fail.failure.grok.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import fail.failure.grok.MainActivity

object WidgetNavigation {
    val DESTINATION_KEY = ActionParameters.Key<String>("destination")
    val AGENT_ID_KEY = ActionParameters.Key<String>("agentId")
    const val DESTINATION_NEW_AGENT = "new_agent"
    const val DESTINATION_OPEN_AGENT = "open_agent"
}

class NewAgentWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF111111))
                    .cornerRadius(28.dp)
                    .clickable(
                        actionStartActivity<MainActivity>(
                            actionParametersOf(WidgetNavigation.DESTINATION_KEY to WidgetNavigation.DESTINATION_NEW_AGENT),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = GlanceModifier.padding(8.dp),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                ) {
                    Text(
                        "+",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFF2F2F2)),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        "New chat",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFF2F2F2)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        }
    }
}

class NewAgentWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NewAgentWidget()
}
