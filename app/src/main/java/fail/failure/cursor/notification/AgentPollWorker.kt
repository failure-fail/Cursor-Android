package fail.failure.cursor.notification

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import fail.failure.cursor.CursorApp
import fail.failure.cursor.widget.AgentStatusWidget

private val TERMINAL_STATES = setOf("finished", "completed", "failed", "error", "cancelled", "needs_input")
private const val PREFS_NAME = "agent_poll_state"

/**
 * Periodically checks each agent's latest run status and notifies on any transition into a
 * terminal or needs-input state - this is what keeps agents you started earlier (or that are
 * running on your own machine via Remote Control) surfaced even when the app isn't open, similar
 * in spirit to the desktop/iOS app's push notifications.
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
                    if (lastSeenStatus != null && run.status.lowercase() in TERMINAL_STATES) {
                        Notifications.showTerminalNotification(
                            applicationContext,
                            agent.id,
                            agent.name ?: agent.id,
                            run.status,
                        )
                    }
                }
            }
            AgentStatusWidget().updateAll(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
