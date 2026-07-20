package fail.failure.cursor.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
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
import fail.failure.cursor.CursorApp
import fail.failure.cursor.ui.agents.AgentDetailScreen
import fail.failure.cursor.ui.agents.AgentDetailViewModel
import fail.failure.cursor.ui.agents.AgentListScreen
import fail.failure.cursor.ui.agents.AgentUsageScreen
import fail.failure.cursor.ui.agents.AgentUsageViewModel
import fail.failure.cursor.ui.agents.AgentsViewModel
import fail.failure.cursor.ui.agents.NewAgentScreen
import fail.failure.cursor.ui.agents.NewAgentViewModel
import fail.failure.cursor.ui.auth.AuthViewModel
import fail.failure.cursor.ui.auth.LoginScreen
import fail.failure.cursor.ui.auth.OnboardingScreen
import fail.failure.cursor.ui.components.BottomNavItem
import fail.failure.cursor.ui.components.CursorBottomBar
import fail.failure.cursor.ui.settings.SettingsScreen

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
fun CursorNavHost(app: CursorApp, requestedDestination: String? = null) {
    val navController: NavHostController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(
        factory = LambdaViewModelFactory { AuthViewModel(app.authRepository) },
    )
    val startDestination = when {
        app.authRepository.isSignedIn() -> Routes.AGENTS
        !app.onboardingPrefs.hasSeenIntro() -> Routes.ONBOARDING
        else -> Routes.LOGIN
    }

    LaunchedEffect(requestedDestination) {
        if (requestedDestination == "new_agent" && app.authRepository.isSignedIn()) {
            navController.navigate(Routes.NEW_AGENT)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == Routes.AGENTS || currentRoute == Routes.SETTINGS

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                CursorBottomBar(
                    items = listOf(
                        BottomNavItem(Routes.AGENTS, Icons.Filled.SmartToy, "Agents"),
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
                factory = LambdaViewModelFactory { AgentsViewModel(app.apiClient, app.authRepository) },
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
                factory = LambdaViewModelFactory { AgentDetailViewModel(app.apiClient, agentId, app) },
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
            SettingsScreen(
                authRepository = app.authRepository,
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
