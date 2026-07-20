package fail.failure.grok

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import fail.failure.grok.agents.PinnedAgentsStore
import fail.failure.grok.agents.TranscriptStore
import fail.failure.grok.auth.AuthRepository
import fail.failure.grok.auth.GrokOAuthClient
import fail.failure.grok.auth.TokenStore
import fail.failure.grok.network.ApiClient
import fail.failure.grok.notification.AgentPollWorker
import fail.failure.grok.notification.Notifications
import fail.failure.grok.onboarding.OnboardingPrefs
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Hand-rolled DI container: this app is small enough that a framework would be overhead. */
class GrokApp : Application() {

    lateinit var tokenStore: TokenStore
        private set
    lateinit var apiClient: ApiClient
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var onboardingPrefs: OnboardingPrefs
        private set
    lateinit var pinnedAgentsStore: PinnedAgentsStore
        private set
    lateinit var transcriptStore: TranscriptStore
        private set

    override fun onCreate() {
        super.onCreate()
        tokenStore = TokenStore(this)
        apiClient = ApiClient(tokenStore)
        authRepository = AuthRepository(tokenStore, GrokOAuthClient(OkHttpClient()))
        onboardingPrefs = OnboardingPrefs(this)
        pinnedAgentsStore = PinnedAgentsStore(this)
        transcriptStore = TranscriptStore(this)
        Notifications.ensureChannel(this)
        schedulePolling()
    }

    private fun schedulePolling() {
        val request = PeriodicWorkRequestBuilder<AgentPollWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "agent_status_poll",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
