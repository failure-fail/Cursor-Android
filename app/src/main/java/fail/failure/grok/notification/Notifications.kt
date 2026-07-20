package fail.failure.grok.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import fail.failure.grok.MainActivity
import fail.failure.grok.R

private val RUNNING_STATES = setOf("running", "starting", "pending")

object Notifications {
    const val CHANNEL_ID = "agent_status"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Agent status",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Live status while a Grok background agent runs, and when it finishes or needs input"
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Posts (or updates in place) a non-dismissible "live" notification while an agent's run is
     * active - the closest Android equivalent to iOS's lock-screen Live Activity for the same
     * feature, since Android has no direct cross-version equivalent to that API.
     */
    fun showRunProgress(context: Context, agentId: String, agentName: String, status: String?, latestText: String?) {
        if (!hasPermission(context)) return
        val isRunning = status?.lowercase() in RUNNING_STATES

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_status_dot)
            .setContentTitle(agentName)
            .setContentText(latestText?.take(120) ?: statusLabel(status))
            .setOngoing(isRunning)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAgentIntent(context, agentId))
            .setAutoCancel(!isRunning)

        if (isRunning) {
            builder.setProgress(0, 0, true)
        }

        NotificationManagerCompat.from(context).notify(notificationId(agentId), builder.build())
    }

    fun showTerminalNotification(context: Context, agentId: String, agentName: String, status: String) {
        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_status_dot)
            .setContentTitle(agentName)
            .setContentText("Agent ${statusLabel(status)}")
            .setContentIntent(openAgentIntent(context, agentId))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(agentId), notification)
    }

    private fun statusLabel(status: String?) = status?.replaceFirstChar { it.uppercase() } ?: "Updated"

    private fun notificationId(agentId: String) = agentId.hashCode()

    private fun openAgentIntent(context: Context, agentId: String): PendingIntent {
        val activityIntent = android.content.Intent(context, MainActivity::class.java)
        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(activityIntent)
            getPendingIntent(agentId.hashCode(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                ?: PendingIntent.getActivity(
                    context,
                    agentId.hashCode(),
                    activityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
        }
    }

    private fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
