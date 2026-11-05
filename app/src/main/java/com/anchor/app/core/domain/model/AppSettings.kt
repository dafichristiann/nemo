package com.anchor.app.core.domain.model

/**
 * The user's top priority, chosen during onboarding (docs 07-screens.md §2.3b).
 *
 * Drives personalization: the mascot greeting nudges toward this area, and the value is stored
 * as `String` in Room (via `.name`) so adding new areas never requires a migration.
 */
enum class FocusArea { COLLEGE, CAREER, PERSONAL_GROWTH }

/**
 * App-wide settings (docs/superpowers/design/03-database-schema.md §7.1).
 * Defaults are privacy-preserving: tracking & AI OFF until the user opts in (FR-S6).
 *
 * `@Immutable`: `blockedApps` is a read-only set that is replaced (via `copy`) rather than mutated.
 */
@androidx.compose.runtime.Immutable
data class AppSettings(
    val trackingEnabled: Boolean = false,
    val aiEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val usageRetentionDays: Int = 30,
    val onboardingCompleted: Boolean = false,
    val locale: String = "en",
    /** Apps to pause during Strict/Custom focus (FR-S4). Stored as a set. */
    val blockedApps: Set<String> = emptySet(),
    /**
     * Opt-in master switch for Strict/Custom app blocking (FR-F4/F8, PRD v0.4 §10.5).
     * Default OFF: privacy-preserving, per Spike B "feature-flagged & opt-in".
     */
    val focusBlockingEnabled: Boolean = false,
    /** Preferred name the mascot uses to greet the user. Blank = no nickname set. */
    val nickname: String = "",
    /** The user's main focus area (docs 07-screens.md §2.3b). Defaults to College. */
    val focusArea: FocusArea = FocusArea.COLLEGE,
    /** Default focus-session length in minutes (25/45/60). Prefills Focus Setup. */
    val defaultFocusDurationMin: Int = DEFAULT_FOCUS_DURATION_MIN,
    /** Opt-in switch for continuous doomscroll alerts on social/entertainment apps. */
    val doomscrollAlertEnabled: Boolean = false,
    /** Continuous scrolling duration before intervention overlay triggers (minutes). */
    val doomscrollThresholdMinutes: Int = DEFAULT_DOOMSCROLL_THRESHOLD_MIN,
    /** Override for today's wake time epoch day (Sprint 8 / Adaptive Circadian). */
    val manualWakeTimeEpochDay: Long? = null,
    /** Override for wake time in minutes of day (e.g. 12 * 60 + 30 = 750 for 12:30). */
    val manualWakeTimeMinutes: Int? = null,
) {
    companion object {
        const val MIN_RETENTION_DAYS = 14
        const val MAX_RETENTION_DAYS = 30

        /** Onboarding focus-duration choices (docs 07-screens.md §2.3b). */
        val FOCUS_DURATION_OPTIONS_MIN = listOf(25, 45, 60)
        const val DEFAULT_FOCUS_DURATION_MIN = 25

        /** Longest nickname we accept, to keep the greeting and the field tidy. */
        const val MAX_NICKNAME_LENGTH = 24

        const val DEFAULT_DOOMSCROLL_THRESHOLD_MIN = 20
        val DOOMSCROLL_DURATION_OPTIONS_MIN = listOf(1, 10, 15, 20, 30, 45)
        val DOOMSCROLL_THRESHOLD_OPTIONS_MIN = DOOMSCROLL_DURATION_OPTIONS_MIN
    }
}
