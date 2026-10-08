package com.anchor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.AnchorTheme
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
        setContent {
            AnchorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AnchorColors.surface,
                ) {
                    AnchorNavHost()
                }
            }
        }
    }
}
