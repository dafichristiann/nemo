package com.anchor.app.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anchor.app.MainActivity
import com.anchor.app.R
import com.anchor.app.core.domain.model.Insight
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.core.domain.usecase.insight.GenerateDailyReflectionUseCase
import com.anchor.app.platform.notification.NotificationChannels
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Generates the Daily Reflection once per day (FR-DR1). Safe to run repeatedly: the use case
 * caches per day, so a second run in the same day is a no-op unless forced.
 *
 * Never fails hard — invalid/again-requested AI responses already fall back to rule-based, so a
 * success result is returned after the (always-available) local generation.
 */
@HiltWorker
class DailyReflectionWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val generateDailyReflection: GenerateDailyReflectionUseCase,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val insight = generateDailyReflection(force = true)
        postNotification(insight)
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        if (runAttemptCount >= 3) Result.failure() else Result.retry()
    }

    private suspend fun postNotification(insight: Insight) {
        if (!settingsRepository.get().notificationsEnabled) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentText = insight.summary.ifBlank {
            "Lihat insight dan refleksi produktivitas Anda hari ini."
        }

        val openIntent = PendingIntent.getActivity(
            appContext,
            insight.id.hashCode(),
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_ROUTE, "insights")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appContext, NotificationChannels.DAILY_REFLECTION)
            .setSmallIcon(R.drawable.ic_stat_nemo)
            .setContentTitle("Refleksi Harian Siap")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val UNIQUE_NAME = "daily_reflection"
        private const val NOTIFICATION_ID = 5101
    }
}
