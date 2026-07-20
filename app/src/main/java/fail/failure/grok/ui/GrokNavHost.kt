package fail.failure.grok.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fail.failure.grok.GrokApp
import fail.failure.grok.ui.agents.AgentDetailScreen
import fail.failure.grok.ui.agents.AgentDetailViewModel
import fail.failure.grok.ui.agents.AgentListScreen
import fail.failure.grok.ui.agents.AgentUsageScreen
import fail.failure.grok.ui.agents.AgentUsageViewModel
import fail.failure.grok.ui.agents.AgentsViewModel
import fail.failure.grok.ui.agents.NewAgentScreen
import fail.failure.grok.ui.agents.NewAgentViewModel
import fail.failure.grok.ui.auth.AuthViewModel
import fail.failure.grok.ui.auth.LoginScreen
import fail.failure.grok.ui.auth.OnboardingScreen
import fail.failure.grok.ui.components.BottomNavItem
import fail.failure.grok.ui.components.GrokBottomBar
import fail.failure.grok.ui.settings.SettingsScreen
import fail.failure.grok.ui.settings.SettingsViewModel

private object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val AGENTS = "agents"
    const val NEW_AGENT = "new_agent"
    const val SETTINGS = "settings"
    const val AGENT_DETAIL = "agent/{agentId}"
    const val AGENT_USAGE = "agent/{agentId}/usage"
    fun agentDetail(agentId: String) = "agent/$agentId"
    fun agentUsage(agentId: String) = "agent/$agentId/usage"
}

@Composable
fun GrokNavHost(app: GrokApp, requestedDestination: String? = null, requestedAgentId: String? = null) {
    val navController: NavHostController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(
        factory = LambdaViewModelFactory { AuthViewModel(app.authRepository) },
    )
    val startDestination = when {
        app.authRepository.isSignedIn() -> Routes.AGENTS
        !app.onboardingPrefs.hasSeenIntro() -> Routes.ONBOARDING
        else -> Routes.LOGIN
    }

    LaunchedEffect(requestedDestination, requestedAgentId) {
        if (!app.authRepository.isSignedIn()) return@LaunchedEffect
        when {
            requestedDestination == "new_agent" -> navController.navigate(Routes.NEW_AGENT)
            requestedDestination == "open_agent" && requestedAgentId != null ->
                navController.navigate(Routes.agentDetail(requestedAgentId))
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == Routes.AGENTS || currentRoute == Routes.SETTINGS

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                GrokBottomBar(
                    items = listOf(
                        BottomNavItem(Routes.AGENTS, Icons.Filled.ChatBubbleOutline, "Chats"),
                        BottomNavItem(Routes.SETTINGS, Icons.Filled.Settings, "Settings"),
                    ),
                    currentRoute = currentRoute,
                    onSelect = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Routes.AGENTS) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        },
    ) { scaffoldPadding ->
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(bottom = if (showBottomBar) scaffoldPadding.calculateBottomPadding() else 0.dp),
        enterTransition = { slideInHorizontally(initialOffsetX = { it / 4 }) + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut() },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onContinue = {
                    app.onboardingPrefs.markIntroSeen()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onSignedIn = {
                    navController.navigate(Routes.AGENTS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.AGENTS) {
            val agentsViewModel: AgentsViewModel = viewModel(
                factory = LambdaViewModelFactory {
                    AgentsViewModel(app.apiClient, app.authRepository, app.pinnedAgentsStore)
                },
            )
            AgentListScreen(
                viewModel = agentsViewModel,
                onOpenAgent = { navController.navigate(Routes.agentDetail(it)) },
                onNewAgent = { navController.navigate(Routes.NEW_AGENT) },
            )
        }

        composable(Routes.NEW_AGENT) {
            val newAgentViewModel: NewAgentViewModel = viewModel(
                factory = LambdaViewModelFactory { NewAgentViewModel(app.apiClient, app.authRepository) },
            )
            NewAgentScreen(
                viewModel = newAgentViewModel,
                onBack = { navController.popBackStack() },
                onCreated = { agentId ->
                    navController.navigate(Routes.agentDetail(agentId)) {
                        popUpTo(Routes.AGENTS)
                    }
                },
            )
        }

        composable(Routes.AGENT_DETAIL) { backStackEntry ->
            val agentId = backStackEntry.arguments?.getString("agentId") ?: return@composable
            val detailViewModel: AgentDetailViewModel = viewModel(
                factory = LambdaViewModelFactory { AgentDetailViewModel(app.apiClient, agentId, app, app.transcriptStore) },
            )
            AgentDetailScreen(
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() },
                onOpenUsage = { navController.navigate(Routes.agentUsage(agentId)) },
            )
        }

        composable(Routes.AGENT_USAGE) { backStackEntry ->
            val agentId = backStackEntry.arguments?.getString("agentId") ?: return@composable
            val usageViewModel: AgentUsageViewModel = viewModel(
                factory = LambdaViewModelFactory { AgentUsageViewModel(app.apiClient, agentId) },
            )
            AgentUsageScreen(viewModel = usageViewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = LambdaViewModelFactory { SettingsViewModel(app.apiClient, app.authRepository) },
            )
            SettingsScreen(
                authRepository = app.authRepository,
                viewModel = settingsViewModel,
                onSignedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0)
                    }
                },
            )
        }
    }
    }
}
