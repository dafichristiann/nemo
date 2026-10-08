package com.anchor.app.platform.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification availability + channel setup (docs/superpowers/design/04-intervention-engine.md §8,
 * 08-permissions.md). `POST_NOTIFICATIONS` is only runtime-granted from Android 13 (API 33); on
 * older versions notifications are available by default.
 *
 * When notifications are unavailable the engine falls back to the in-app channel (FR-I3).
 */
@Singleton
class NotificationPermissions @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** True when a notification may be posted right now (channel + runtime grant). */
    fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
