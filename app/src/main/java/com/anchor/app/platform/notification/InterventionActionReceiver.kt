package com.anchor.app.platform.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anchor.app.core.domain.model.InterventionAction
import com.anchor.app.core.domain.usecase.intervention.InterventionEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles the notification's action buttons (FR-I7). [Start Focus] records `INTERACTED` and
 * routes to Focus; [Later] records `DISMISSED` and arms a 2-hour cooldown for that context
 * (docs/superpowers/design/04-intervention-engine.md §3.5).
 *
 * Runs off the main thread via an application-scoped coroutine; failures are swallowed so the
 * receiver never crashes the process (graceful degradation, docs 02 §6).
 */
@AndroidEntryPoint
class InterventionActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var engine: InterventionEngine

    override fun onReceive(context: Context, intent: Intent) {
        val logId = intent.getStringExtra(EXTRA_LOG_ID) ?: return
        val action = intent.getStringExtra(EXTRA_ACTION)
            ?.let { name -> InterventionAction.entries.firstOrNull { it.name == name } }
            ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                engine.recordAction(logId, action)
                androidx.core.app.NotificationManagerCompat.from(context).cancel(logId.hashCode())
                if (action == InterventionAction.START_FOCUS) {
                    val relatedTaskId = intent.getStringExtra(EXTRA_TASK_ID)
                    val route = if (!relatedTaskId.isNullOrBlank()) "focus?taskId=$relatedTaskId" else "focus"
                    val activityIntent = Intent(context, com.anchor.app.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(com.anchor.app.MainActivity.EXTRA_OPEN_ROUTE, route)
                    }
                    context.startActivity(activityIntent)
                }
            }
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION = "com.anchor.app.INTERVENTION_ACTION"
        const val EXTRA_LOG_ID = "extra_log_id"
        const val EXTRA_ACTION = "extra_action"
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
