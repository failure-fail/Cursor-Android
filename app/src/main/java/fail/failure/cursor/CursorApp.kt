package fail.failure.cursor

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import fail.failure.cursor.auth.AuthRepository
import fail.failure.cursor.auth.DeepLinkAuthClient
import fail.failure.cursor.auth.TokenStore
import fail.failure.cursor.network.ApiClient
import fail.failure.cursor.notification.AgentPollWorker
import fail.failure.cursor.notification.Notifications
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Hand-rolled DI container: this app is small enough that a framework would be overhead. */
class CursorApp : Application() {

    lateinit var tokenStore: TokenStore
        private set
    lateinit var apiClient: ApiClient
        private set
    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        tokenStore = TokenStore(this)
        apiClient = ApiClient(tokenStore)
        authRepository = AuthRepository(tokenStore, DeepLinkAuthClient(OkHttpClient()))
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
