package com.anchor.app.core.domain.usecase.focus

import com.anchor.app.core.domain.repository.FocusRepository
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Persists session rating and provides z-score individualized normalization (Spec 2026-11-03 §3.2).
 */
class RateFocusSessionUseCase @Inject constructor(
    private val focusRepository: FocusRepository,
) {
    suspend operator fun invoke(sessionId: String, rating: Int) {
        val clamped = rating.coerceIn(1, 5)
        focusRepository.updateRating(sessionId, clamped)
    }

    companion object {
        const val MIN_DURATION_FOR_RATING_MS = 15 * 60 * 1000L

        /**
         * Normalizes a rating in [1..5] to [0.0..1.0].
         * Uses linear scaling when samples M < 5, otherwise z-score clamping.
         */
        fun normalize(rating: Int, allRatings: List<Int>): Float {
            val clampedRating = rating.coerceIn(1, 5)
            if (allRatings.size < 5) {
                return (clampedRating - 1) / 4f
            }

            val m = allRatings.size.toDouble()
            val mean = allRatings.sum().toDouble() / m
            val variance = allRatings.sumOf { (it - mean) * (it - mean) } / m
            val stdDev = sqrt(variance)

            val normalized = 0.5 + (clampedRating - mean) / (2.0 * (stdDev + 0.1))
            return normalized.toFloat().coerceIn(0f, 1f)
        }
    }
}
