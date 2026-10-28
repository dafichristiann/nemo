package com.anchor.app

import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.anchor.app.feature.splash.SplashScreen
import com.anchor.app.navigation.NemoNavHost
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

    private val openRouteState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Optional deep-link from the focus-block overlay ("Back to focus" → Focus tab).
        openRouteState.value = intent?.getStringExtra(EXTRA_OPEN_ROUTE)
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
                        if (showSplash) {
                            SplashScreen(onFinished = { showSplash = false })
                        } else {
                            NemoNavHost(initialRoute = currentOpenRoute)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_OPEN_ROUTE)?.let {
            openRouteState.value = it
        }
    }

    companion object {
        /** Extra carrying a top-level route to open on launch (e.g. "focus"). */
        const val EXTRA_OPEN_ROUTE = "com.anchor.app.extra.OPEN_ROUTE"
    }
}
