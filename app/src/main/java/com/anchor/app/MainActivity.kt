package com.anchor.app

import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.anchor.app.core.domain.usecase.intervention.InterventionResponse
import com.anchor.app.core.domain.usecase.intervention.RecordInterventionResponseUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.anchor.app.core.design.theme.AppThemeMode
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme
import com.anchor.app.core.design.theme.ThemePreferences
import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.feature.splash.NemoSplash
import com.anchor.app.navigation.NemoNavHost
import com.anchor.app.navigation.Routes
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

/**
 * Single entry activity hosting the Compose UI (declared in AndroidManifest.xml with
 * the LAUNCHER intent-filter).
 *
 * @AndroidEntryPoint lets future ViewModels be Hilt-injected. Sprint 0 only renders the
 * navigation shell.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var recordInterventionResponse: RecordInterventionResponseUseCase

    private val openRouteState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleInterventionIntent(intent)
        // Deep-link from notification or focus-block overlay
        openRouteState.value = resolveRouteFromIntent(intent)
        setContent {
            val themeMode by themePreferences.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            fun isIndonesian(locale: String) =
                locale.startsWith("id", ignoreCase = true) || locale.startsWith("in", ignoreCase = true)

            val initialLocale = remember {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val lm = getSystemService(android.app.LocaleManager::class.java)
                    val appLocales = lm?.applicationLocales
                    if (appLocales != null && !appLocales.isEmpty) {
                        val lang = appLocales.get(0)?.language ?: "en"
                        if (isIndonesian(lang)) "id" else lang
                    } else {
                        "en"
                    }
                } else {
                    "en"
                }
            }

            val settings by settingsRepository.observe().collectAsState(initial = AppSettings(locale = initialLocale))
            val targetLocale = remember(settings.locale) {
                if (isIndonesian(settings.locale)) Locale("in", "ID") else Locale.ENGLISH
            }

            val context = LocalContext.current
            val localizedContext = remember(context, targetLocale) {
                Locale.setDefault(targetLocale)
                val config = Configuration(context.resources.configuration).apply {
                    setLocale(targetLocale)
                    setLayoutDirection(targetLocale)
                }
                val configContext = context.createConfigurationContext(config)
                object : ContextWrapper(context) {
                    override fun getResources(): Resources = configContext.resources
                }
            }
            val localizedConfig = remember(localizedContext) {
                localizedContext.resources.configuration
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfig,
            ) {
                NemoTheme(darkTheme = isDark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Nemo.colors.background,
                    ) {
                        // Show the branded intro clip once per cold start, then the shell. Skipped on
                        // configuration change so a rotation doesn't replay the splash.
                        var showSplash by remember { mutableStateOf(savedInstanceState == null) }
                        val currentOpenRoute by openRouteState

                        Box(modifier = Modifier.fillMaxSize()) {
                            NemoNavHost(
                                initialRoute = currentOpenRoute,
                                onRouteHandled = { openRouteState.value = null },
                            )
                            if (showSplash) {
                                NemoSplash(
                                    targetBounds = null,
                                    isReady = true,
                                    onFinished = { showSplash = false },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleInterventionIntent(intent)
        resolveRouteFromIntent(intent)?.let {
            openRouteState.value = it
        }
    }

    private fun handleInterventionIntent(intent: Intent?) {
        if (intent == null) return
        val logId = intent.getStringExtra(EXTRA_LOG_ID) ?: return
        val action = intent.getStringExtra(EXTRA_ACTION) ?: return
        if (action.equals(ACTION_START_FOCUS, ignoreCase = true) ||
            action.equals("ACTION_START_FOCUS", ignoreCase = true) ||
            action.equals(ACTION_OPEN_TASK, ignoreCase = true) ||
            action.equals("ACTION_OPEN_TASK", ignoreCase = true)
        ) {
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    recordInterventionResponse(logId, InterventionResponse.OPENED)
                }
            }
        }
    }

    private fun resolveRouteFromIntent(intent: Intent?): String? {
        if (intent == null) return null

        val logId = intent.getStringExtra(EXTRA_LOG_ID)
        val action = intent.getStringExtra(EXTRA_ACTION)
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)
            ?: intent.getStringExtra(EXTRA_TASK_ID_ALT)
        val explicitRoute = intent.getStringExtra(EXTRA_OPEN_ROUTE)

        return when {
            action.equals(ACTION_START_FOCUS, ignoreCase = true) ||
            action.equals("ACTION_START_FOCUS", ignoreCase = true) -> {
                if (!taskId.isNullOrBlank()) Routes.focus(taskId) else "focus"
            }
            action.equals(ACTION_OPEN_TASK, ignoreCase = true) ||
            action.equals("ACTION_OPEN_TASK", ignoreCase = true) -> {
                if (!taskId.isNullOrBlank()) Routes.taskDetail(taskId) else "tasks"
            }
            !taskId.isNullOrBlank() -> {
                Routes.taskDetail(taskId)
            }
            !explicitRoute.isNullOrBlank() -> {
                explicitRoute
            }
            else -> null
        }
    }

    companion object {
        /** Extra carrying a top-level route to open on launch (e.g. "focus"). */
        const val EXTRA_OPEN_ROUTE = "com.anchor.app.extra.OPEN_ROUTE"
        const val EXTRA_LOG_ID = "extra_log_id"
        const val EXTRA_ACTION = "extra_action"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_ID_ALT = "taskId"

        const val ACTION_OPEN_TASK = "open_task"
        const val ACTION_START_FOCUS = "start_focus"
    }
}
