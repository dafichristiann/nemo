package com.anchor.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.anchor.app.core.design.components.NemoNavBar
import com.anchor.app.core.design.navigation.TopLevelDestination
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.feature.focus.FocusScreen
import com.anchor.app.feature.futureview.FutureViewScreen
import com.anchor.app.feature.futureview.FutureViewViewModel
import com.anchor.app.feature.insights.InsightsScreen
import com.anchor.app.feature.intervention.InterventionOverlay
import com.anchor.app.feature.onboarding.PermissionOnboardingScreen
import com.anchor.app.feature.onboarding.ProfileSetupScreen
import com.anchor.app.feature.onboarding.WelcomeOnboardingScreen
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
    const val WELCOME_ONBOARDING = "onboarding/welcome"
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
fun NemoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    initialRoute: String? = null,
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val onboardingCompleted by appViewModel.onboardingCompleted.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentRouteBase = currentRoute?.substringBefore('?')
    val isTopLevel = TopLevelDestination.entries.any { it.route == currentRouteBase }

    // Wait until we know whether onboarding is done (avoids a wrong-jump flash). When onboarding
    // is complete, land on deepLink or Tasks.
    val deepLink = initialRoute?.takeIf { r -> TopLevelDestination.entries.any { it.route == r.substringBefore('?') } }
    val startDestination = when {
        onboardingCompleted == null -> null
        onboardingCompleted == false -> Routes.WELCOME_ONBOARDING
        else -> deepLink ?: TopLevelDestination.TASKS.route
    }

    LaunchedEffect(initialRoute, onboardingCompleted) {
        if (initialRoute != null && onboardingCompleted == true) {
            navController.navigate(initialRoute) {
                popUpTo(TopLevelDestination.TASKS.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (isTopLevel) {
                NemoNavBar(
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

        val isTopLevelRoute: (String?) -> Boolean = { route ->
            val base = route?.substringBefore('?')
            TopLevelDestination.entries.any { it.route == base }
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                if (isTopLevelRoute(initialRoute) && isTopLevelRoute(targetRoute)) {
                    fadeIn(animationSpec = Motion.standardSpec())
                } else {
                    fadeIn(animationSpec = Motion.standardSpec()) +
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = Motion.standardSpec(),
                            initialOffset = { (it * 0.08f).toInt() },
                        )
                }
            },
            exitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                if (isTopLevelRoute(initialRoute) && isTopLevelRoute(targetRoute)) {
                    fadeOut(animationSpec = Motion.instantSpec())
                } else {
                    fadeOut(animationSpec = Motion.instantSpec()) +
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = Motion.standardSpec(),
                            targetOffset = { -(it * 0.08f).toInt() },
                        )
                }
            },
            popEnterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                if (isTopLevelRoute(initialRoute) && isTopLevelRoute(targetRoute)) {
                    fadeIn(animationSpec = Motion.standardSpec())
                } else {
                    fadeIn(animationSpec = Motion.standardSpec()) +
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = Motion.standardSpec(),
                            initialOffset = { -(it * 0.08f).toInt() },
                        )
                }
            },
            popExitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                if (isTopLevelRoute(initialRoute) && isTopLevelRoute(targetRoute)) {
                    fadeOut(animationSpec = Motion.instantSpec())
                } else {
                    fadeOut(animationSpec = Motion.instantSpec()) +
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = Motion.standardSpec(),
                            targetOffset = { (it * 0.08f).toInt() },
                        )
                }
            },
        ) {
            val finishOnboarding = {
                appViewModel.completeOnboarding()
                navController.navigate(TopLevelDestination.TASKS.route) {
                    popUpTo(Routes.WELCOME_ONBOARDING) { inclusive = true }
                }
            }

            composable(Routes.WELCOME_ONBOARDING) {
                WelcomeOnboardingScreen(
                    onGetStarted = {
                        navController.navigate(Routes.PROFILE_ONBOARDING)
                    },
                    onSkip = finishOnboarding,
                )
            }
            composable(Routes.PROFILE_ONBOARDING) {
                ProfileSetupScreen(
                    onContinue = {
                        if (appViewModel.allRequiredPermissionsGranted()) {
                            finishOnboarding()
                        } else {
                            navController.navigate(Routes.PERMISSION_ONBOARDING)
                        }
                    },
                    onSkip = finishOnboarding,
                )
            }
            composable(Routes.PERMISSION_ONBOARDING) {
                PermissionOnboardingScreen(
                    onContinue = finishOnboarding,
                    onSkip = finishOnboarding,
                )
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
            // Focus route handles both the bare tab route ("focus") and the task-scoped route
            // ("focus?taskId={taskId}") via an optional query parameter.
            val focusContent: @Composable () -> Unit = {
                val context = LocalContext.current
                FocusScreen(
                    onManageBlockedApps = { navController.navigate(Routes.SETTINGS_BLOCKED) },
                    onOpenAccessibilitySettings = { AccessibilityStatus().open(context) },
                    // After finishing a session (the task is now done) land the user on Tasks so
                    // they immediately see the completed item — no dead-end on the Focus tab.
                    onFinished = {
                        navController.navigate(TopLevelDestination.TASKS.route) {
                            // Pop back to the Tasks tab without leaving a stale Focus entry behind,
                            // whether Focus was reached from the bar or a task-scoped deep link.
                            popUpTo(TopLevelDestination.TASKS.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }
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
