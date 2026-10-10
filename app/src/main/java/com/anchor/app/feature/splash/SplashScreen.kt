package com.anchor.app.feature.splash

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.anchor.app.R
import kotlinx.coroutines.delay

/**
 * Nemo splash screen: plays the branded intro clip (light or dark variant, matching the system
 * theme) with its short chime, then hands off to [onFinished].
 *
 * Safety net: even if the clip fails to load (unsupported codec, decode error) or the surface
 * never reports the end, a bounded timeout guarantees we always advance — the splash can never
 * trap the user on a blank screen.
 */
@androidx.annotation.OptIn(UnstableApi::class) // media3 PlayerView / AspectRatioFrameLayout are @UnstableApi.
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) Color(0xFF0A0A0A) else Color(0xFFFFFFFF)
    val videoRes = if (isDark) R.raw.nemo_splash_dark else R.raw.nemo_splash_light

    // Single player for the lifetime of the splash. Muted is not wanted here — the clip has its
    // own short chime, so volume stays on.
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(
                MediaItem.fromUri(
                    Uri.parse("android.resource://${context.packageName}/$videoRes"),
                ),
            )
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 1f
            prepare()
            playWhenReady = true
        }
    }

    // Advance once the clip finishes; the timeout is a fallback if it never reports an end.
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) onFinished()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // Never block the app on a failed splash: fall through immediately.
                onFinished()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Hard ceiling: the clip is ~3.5s; give a little headroom before forcing the handoff.
    LaunchedEffect(Unit) {
        delay(SPLASH_MAX_MS)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    // Fill the screen without distorting the clip.
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    this.player = player
                }
            },
        )
    }
}

/** Fallback ceiling (ms) — comfortably above the ~3.5s clip length. */
private const val SPLASH_MAX_MS = 4_500L
