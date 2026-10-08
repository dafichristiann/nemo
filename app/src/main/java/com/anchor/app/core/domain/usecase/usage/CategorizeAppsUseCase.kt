package com.anchor.app.core.domain.usecase.usage

import com.anchor.app.core.domain.model.AppCategory
import com.anchor.app.core.domain.repository.UsageRepository
import javax.inject.Inject

/**
 * Category constants + a small starter of well-known packages (FR-S3).
 *
 * The list is intentionally short and honest: most apps fall to [OTHER] until the user
 * assigns a category. Categories are never used to judge — only to compute a distraction
 * ratio and power the Wellbeing breakdown (PRD §10.2, principle "Awareness > Control").
 */
object AppCategories {
    const val PRODUCTIVE = "PRODUCTIVE"
    const val SOCIAL = "SOCIAL"
    const val ENTERTAINMENT = "ENTERTAINMENT"
    const val OTHER = "OTHER"

    /** Packages commonly treated as distraction by default (still user-overridable). */
    private val DISTRACTION_DEFAULTS = mapOf(
        "com.zhiliaoapp.musically" to SOCIAL,        // TikTok
        "com.ss.android.ugc.trill" to SOCIAL,        // TikTok (alt)
        "com.instagram.android" to SOCIAL,
        "com.facebook.katana" to SOCIAL,
        "com.twitter.android" to SOCIAL,
        "com.x.android" to SOCIAL,
        "com.reddit.frontpage" to ENTERTAINMENT,
        "com.google.android.youtube" to ENTERTAINMENT,
        "com.netflix.mediaclient" to ENTERTAINMENT,
    )

    /** Packages we can safely treat as productive (never counted as distraction). */
    private val PRODUCTIVE_DEFAULTS = setOf(
        "com.android.chrome",
        "com.google.android.apps.docs",
        "com.notion.id",
        "com.google.android.calendar",
        "com.microsoft.office.word",
        "com.google.android.keep",
    )

    /** Best-effort default category for a package, or [OTHER]. */
    fun defaultCategory(packageName: String): String = when {
        packageName in PRODUCTIVE_DEFAULTS -> PRODUCTIVE
        else -> DISTRACTION_DEFAULTS[packageName] ?: OTHER
    }

    /** Whether a package is a distraction by default (false for productive/unknown). */
    fun defaultIsDistraction(packageName: String): Boolean = packageName in DISTRACTION_DEFAULTS
}

/**
 * Auto-assigns categories to apps seen in usage (FR-S3, docs 02 §3.2).
 *
 * Rules:
 *  - Only apps without a category yet are assigned (idempotent).
 *  - A user override ([AppCategory.SOURCE_USER]) is **never** overwritten by an AUTO pass.
 */
class CategorizeAppsUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
) {
    suspend operator fun invoke(packageNames: Collection<String>): Int {
        val existing = usageRepository.appCategories().associateBy { it.packageName }
        val toAssign = packageNames
            .distinct()
            // Skip anything the user has explicitly categorised.
            .filter { pkg -> existing[pkg]?.overrideSource != AppCategory.SOURCE_USER }
            .map { pkg ->
                AppCategory(
                    packageName = pkg,
                    category = AppCategories.defaultCategory(pkg),
                    isDistraction = AppCategories.defaultIsDistraction(pkg),
                    overrideSource = AppCategory.SOURCE_AUTO,
                )
            }
        if (toAssign.isEmpty()) return 0
        usageRepository.upsertAppCategories(toAssign)
        return toAssign.size
    }
}
