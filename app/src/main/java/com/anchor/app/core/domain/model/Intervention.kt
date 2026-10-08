package com.anchor.app.core.domain.model

/**
 * Intervention domain contracts (docs/superpowers/design/04-intervention-engine.md §7).
 *
 * These types are the boundary between the Drift Detector and the Intervention Engine, and
 * between the engine and the UI/channels. Nothing here diagnoses or judges — it records facts
 * and decisions (PRD §4, docs 04 §6).
 */

/** Channel an intervention is delivered through, in priority order (FR-I3, docs 04 §3.4). */
enum class InterventionChannel { NOTIFICATION, IN_APP, FULLSCREEN }

/** Terminal state of an intervention (docs 03 §5.1, PRD §10.4). */
enum class InterventionStatus { INTERACTED, DISMISSED, EXPIRED }

/** What the user did with a shown intervention (docs 04 §7). */
enum class InterventionAction { START_FOCUS, LATER, DISMISS, EXPIRE }

/** Why a drift signal did not become an intervention (docs 04 §3.2). */
enum class SuppressReason { NO_DRIFT, QUOTA_EXHAUSTED, COOLDOWN, USER_IN_APP, NO_PERMISSION }

/** The intervention levels, ascending — never jump straight to [FOCUS] (FR-I2, docs 04 §3.3). */
object InterventionLevel {
    const val GENTLE = 1
    const val CONTEXTUAL = 2
    const val FUTURE_VIEW = 3
    const val FOCUS = 4
}

/** A persisted intervention record (docs 03 §5.1). */
data class InterventionLog(
    val id: String,
    val level: Int,
    val channel: InterventionChannel,
    val contextPackage: String?,
    val relatedTaskId: String?,
    val status: InterventionStatus,
    val shownAt: Long,
    val resolvedAt: Long?,
    val cooldownTriggered: Boolean,
)

/**
 * Engine decision (docs 04 §7). [Show] carries everything a channel needs to render and log;
 * [Suppress] carries the non-judgemental reason it was held back.
 */
sealed interface InterventionDecision {
    data class Show(
        val level: Int,
        val channel: InterventionChannel,
        val signal: DriftSignal,
    ) : InterventionDecision

    data class Suppress(val reason: SuppressReason) : InterventionDecision
}
