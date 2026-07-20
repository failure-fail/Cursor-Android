package fail.failure.cursor.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

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
            description = "Updates when a Cursor background agent finishes or needs input"
        }
        manager.createNotificationChannel(channel)
    }
}
