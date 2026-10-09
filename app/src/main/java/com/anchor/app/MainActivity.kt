package com.anchor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.AnchorTheme
import com.anchor.app.feature.splash.SplashScreen
import com.anchor.app.navigation.AnchorNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single entry activity hosting the Compose UI (declared in AndroidManifest.xml with
 * the LAUNCHER intent-filter).
 *
 * @AndroidEntryPoint lets future ViewModels be Hilt-injected. Sprint 0 only renders the
 * navigation shell.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Optional deep-link from the focus-block overlay ("Back to focus" → Focus tab).
        val initialRoute = intent?.getStringExtra(EXTRA_OPEN_ROUTE)
        setContent {
            AnchorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AnchorColors.surface,
                ) {
                    // Show the branded intro clip once per cold start, then the shell. Skipped on
                    // configuration change so a rotation doesn't replay the splash.
                    var showSplash by remember { mutableStateOf(savedInstanceState == null) }
                    if (showSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else {
                        AnchorNavHost(initialRoute = initialRoute)
                    }
                }
            }
        }
    }

    companion object {
        /** Extra carrying a top-level route to open on launch (e.g. "focus"). */
        const val EXTRA_OPEN_ROUTE = "com.anchor.app.extra.OPEN_ROUTE"
    }
}
