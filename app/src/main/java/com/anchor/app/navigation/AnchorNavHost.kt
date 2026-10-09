package com.anchor.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anchor.app.core.design.components.AnchorNavBar
import com.anchor.app.core.design.navigation.TopLevelDestination
import com.anchor.app.feature.focus.FocusScreen
import com.anchor.app.feature.futureview.FutureViewScreen
import com.anchor.app.feature.futureview.FutureViewViewModel
import com.anchor.app.feature.insights.InsightsScreen
import com.anchor.app.feature.intervention.InterventionOverlay
import com.anchor.app.feature.onboarding.PermissionOnboardingScreen
import com.anchor.app.feature.onboarding.ProfileSetupScreen
import com.anchor.app.feature.settings.AppCategoriesScreen
import com.anchor.app.feature.settings.BlockedAppsScreen
import com.anchor.app.feature.settings.SettingsScreen
import com.anchor.app.feature.tasks.TaskDetailScreen
import com.anchor.app.feature.tasks.TaskDetailViewModel
import com.anchor.app.feature.tasks.TasksScreen
import com.anchor.app.feature.wellbeing.WellbeingScreen
import com.anchor.app.platform.accessibility.AccessibilityStatus

/** All routes (docs 07-screens.md §1: "Detail & modal as separate routes"). */
object Routes {
    const val PROFILE_ONBOARDING = "onboarding/profile"
    const val PERMISSION_ONBOARDING = "onboarding/permissions"
    const val SETTINGS = "settings"
    const val SETTINGS_CATEGORIES = "settings/categories"
    const val SETTINGS_BLOCKED = "settings/blocked"
    const val TASK_DETAIL = "task/{taskId}"
    fun taskDetail(taskId: String) = "task/$taskId"
    const val FUTURE_VIEW = "future/{taskId}"
    fun futureView(taskId: String) = "future/$taskId"

    /**
     * Focus route. The optional `taskId` preselects the task a session is started for
     * (PRD v0.5 §10.1: "start focus for this task" from Detail/FutureView).
     */
    const val FOCUS_TASK_ARG = "taskId"
    const val FOCUS = "focus?$FOCUS_TASK_ARG={$FOCUS_TASK_ARG}"
    fun focus(taskId: String? = null) =
        if (taskId == null) "focus" else "focus?$FOCUS_TASK_ARG=$taskId"
}

/**
 * Navigation shell (docs/superpowers/design/07-screens.md §1, §6).
 *
 * On first launch the flow is 00a Profile Setup → 00b Permission Onboarding → main shell.
 * The main shell has four top-level tabs (Tasks · Wellbeing · Focus · Insights). Detail
 * and Settings routes hide the bottom bar, as in the mockups (back header instead).
 */
@Composable
fun AnchorNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    initialRoute: String? = null,
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val onboardingCompleted by appViewModel.onboardingCompleted.collectAsStateWithLifecycle()

    // Wait until we know whether onboarding is done (avoids a wrong-jump flash). When onboarding
    // is complete and a deep-linked [initialRoute] is a valid top-level tab, land there.
    val deepLink = initialRoute?.takeIf { r -> TopLevelDestination.entries.any { it.route == r } }
    val startDestination = when (onboardingCompleted) {
        null -> null
        false -> Routes.PROFILE_ONBOARDING
        true -> deepLink ?: TopLevelDestination.TASKS.route
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // Focus is reachable both as the bare tab ("focus") and with a preselected task
    // ("focus?taskId={taskId}"), so match on the route *base* (before any query) — otherwise the
    // bottom bar would disappear when Focus is opened for a specific task from Detail/FutureView.
    val currentRouteBase = currentRoute?.substringBefore('?')
    val isTopLevel = TopLevelDestination.entries.any { it.route == currentRouteBase }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (isTopLevel) {
                AnchorNavBar(
                    destinations = TopLevelDestination.entries,
                    currentRoute = currentRouteBase,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(TopLevelDestination.TASKS.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        if (startDestination == null) return@Scaffold

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.PROFILE_ONBOARDING) {
                val finishOnboarding = {
                    appViewModel.completeOnboarding()
                    navController.navigate(TopLevelDestination.TASKS.route) {
                        popUpTo(Routes.PROFILE_ONBOARDING) { inclusive = true }
                    }
                }
                ProfileSetupScreen(
                    onContinue = {
                        // Skip the optional permission step when both permissions are already
                        // granted (e.g. the OS kept the grants); otherwise let the user opt in
                        // (PRD §14 — permissions are optional, never forced).
                        if (appViewModel.optionalPermissionsGranted()) {
                            finishOnboarding()
                        } else {
                            navController.navigate(Routes.PERMISSION_ONBOARDING)
                        }
                    },
                    onSkip = finishOnboarding,
                )
            }
            composable(Routes.PERMISSION_ONBOARDING) {
                val finish = {
                    appViewModel.completeOnboarding()
                    navController.navigate(TopLevelDestination.TASKS.route) {
                        popUpTo(Routes.PROFILE_ONBOARDING) { inclusive = true }
                    }
                }
                PermissionOnboardingScreen(onContinue = finish, onSkip = finish)
            }

            composable(TopLevelDestination.TASKS.route) {
                TasksScreen(
                    onOpenTask = { taskId -> navController.navigate(Routes.taskDetail(taskId)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(TopLevelDestination.WELLBEING.route) {
                WellbeingScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
            }
            // Focus is registered twice by design: the bare tab route ("focus") for bottom-bar
            // navigation, and the task-scoped route ("focus?taskId={taskId}") used when Focus is
            // opened for a specific task from Detail/FutureView (PRD v0.5 §10.1). Both render the
            // same screen; the shared content is factored into `focusContent` to avoid drift.
            val focusContent: @Composable () -> Unit = {
                val context = LocalContext.current
                FocusScreen(
                    onManageBlockedApps = { navController.navigate(Routes.SETTINGS_BLOCKED) },
                    onOpenAccessibilitySettings = { AccessibilityStatus().open(context) },
                )
            }
            composable(TopLevelDestination.FOCUS.route) { focusContent() }
            composable(
                route = Routes.FOCUS,
                arguments = listOf(
                    navArgument(Routes.FOCUS_TASK_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { focusContent() }
            composable(TopLevelDestination.INSIGHTS.route) { InsightsScreen() }

            composable(
                route = Routes.TASK_DETAIL,
                arguments = listOf(navArgument(TaskDetailViewModel.ARG_TASK_ID) { type = NavType.StringType }),
            ) {
                TaskDetailScreen(
                    onBack = { navController.popBackStack() },
                    onStartFocusToComplete = { taskId ->
                        navController.navigate(Routes.focus(taskId)) { launchSingleTop = true }
                    },
                    onOpenFutureView = { taskId -> navController.navigate(Routes.futureView(taskId)) },
                )
            }

            composable(
                route = Routes.FUTURE_VIEW,
                arguments = listOf(navArgument(FutureViewViewModel.ARG_TASK_ID) { type = NavType.StringType }),
            ) {
                FutureViewScreen(
                    onBack = { navController.popBackStack() },
                    onStartFocus = { taskId ->
                        navController.navigate(Routes.focus(taskId)) { launchSingleTop = true }
                    },
                    onOpenTask = { taskId -> navController.navigate(Routes.taskDetail(taskId)) },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onManageCategories = { navController.navigate(Routes.SETTINGS_CATEGORIES) },
                    onManageBlocked = { navController.navigate(Routes.SETTINGS_BLOCKED) },
                )
            }
            composable(Routes.SETTINGS_CATEGORIES) {
                AppCategoriesScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_BLOCKED) {
                BlockedAppsScreen(onBack = { navController.popBackStack() })
            }
        }

        // Screen 04: in-app contextual intervention, drawn above the current destination
        // (docs 07-screens.md §2.8, 04-intervention-engine.md §8). Focus navigation is a
        // Sprint-5 concern; for now the action just records the outcome and closes the overlay.
        InterventionOverlay(
            onNavigateToFocus = { navController.navigate(TopLevelDestination.FOCUS.route) },
        )
    }
}
