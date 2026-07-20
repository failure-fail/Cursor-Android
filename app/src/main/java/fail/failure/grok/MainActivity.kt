package fail.failure.grok

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import fail.failure.grok.ui.GrokNavHost
import fail.failure.grok.ui.theme.GrokAndroidTheme
import fail.failure.grok.widget.WidgetNavigation

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        val app = application as GrokApp
        val requestedDestination = intent?.getStringExtra(WidgetNavigation.DESTINATION_KEY.name)
        val requestedAgentId = intent?.getStringExtra(WidgetNavigation.AGENT_ID_KEY.name)
        setContent {
            GrokAndroidTheme {
                GrokNavHost(app = app, requestedDestination = requestedDestination, requestedAgentId = requestedAgentId)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
