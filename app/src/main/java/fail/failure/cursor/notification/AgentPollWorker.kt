package fail.failure.cursor.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import fail.failure.cursor.CursorApp
import fail.failure.cursor.MainActivity
import fail.failure.cursor.R
import fail.failure.cursor.widget.AgentStatusWidget

private const val TERMINAL_STATES = "finished,completed,failed,error,cancelled,needs_input"
private val PREFS_NAME = "agent_poll_state"

/**
 * Periodically checks each agent's latest run status and notifies on any transition into a
 * terminal or needs-input state - the mobile equivalent of the desktop app's Live Activity /
 * push notification when a background agent needs attention.
 */
class AgentPollWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CursorApp
        if (!app.authRepository.isSignedIn()) return Result.success()

        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        return try {
            val agents = app.apiClient.service.listAgents(limit = 20).agents
            for (agent in agents) {
                val runId = agent.latestRunId ?: continue
                val run = try {
                    app.apiClient.service.getRun(agent.id, runId)
                } catch (_: Exception) {
                    continue
                }
                val lastSeenStatus = prefs.getString(run.id, null)
                if (run.status != lastSeenStatus) {
                    prefs.edit().putString(run.id, run.status).apply()
                    if (lastSeenStatus != null && run.status.lowercase() in TERMINAL_STATES.split(",")) {
                        notify(agent.id, agent.name ?: agent.id, run.status)
                    }
                }
            }
            AgentStatusWidget().updateAll(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun notify(agentId: String, agentName: String, status: String) {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val activityIntent = android.content.Intent(context, MainActivity::class.java)
        val pendingIntent = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(activityIntent)
            getPendingIntent(agentId.hashCode(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_status_dot)
            .setContentTitle(agentName)
            .setContentText("Agent $status")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(agentId.hashCode(), notification)
    }
}
