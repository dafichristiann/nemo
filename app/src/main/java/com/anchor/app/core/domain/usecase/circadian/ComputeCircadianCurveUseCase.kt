package com.anchor.app.core.domain.usecase.circadian

import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.repository.FocusRepository
import com.anchor.app.core.domain.usecase.focus.RateFocusSessionUseCase
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.sqrt

data class SplineSegment(
    val startX: Float,
    val startY: Float,
    val cp1X: Float,
    val cp1Y: Float,
    val cp2X: Float,
    val cp2Y: Float,
    val endX: Float,
    val endY: Float,
)

data class CircadianCurveData(
    val wakeResult: WakeTimeResult,
    val bucketCapacities: List<Float>,
    val segments: List<SplineSegment>,
    val currentCapacity: Float,
    val currentZoneLabel: String,
) {
    /** Evaluates capacity in [0.0..1.0] at hours from wake in [0..24]. */
    fun evaluate(hoursFromWake: Float): Float {
        val h = (hoursFromWake % 24f + 24f) % 24f
        if (segments.isEmpty()) return 0.5f
        val seg = segments.find { h >= it.startX && h <= it.endX } ?: segments.lastOrNull() ?: return 0.5f
        val len = seg.endX - seg.startX
        if (len <= 0f) return seg.startY
        val u = (h - seg.startX) / len
        val u2 = u * u
        val u3 = u2 * u
        val h00 = 2f * u3 - 3f * u2 + 1f
        val h10 = u3 - 2f * u2 + u
        val h01 = -2f * u3 + 3f * u2
        val h11 = u3 - u2
        val d0 = (seg.cp1Y - seg.startY) * 3f / len
        val d1 = (seg.endY - seg.cp2Y) * 3f / len
        val y = h00 * seg.startY + h10 * len * d0 + h01 * seg.endY + h11 * len * d1
        return y.coerceIn(0f, 1f)
    }

    val peakStart: LocalTime
        get() = wakeResult.time.plusMinutes(120) // T+2.0h

    val peakEnd: LocalTime
        get() = wakeResult.time.plusMinutes(270) // T+4.5h

    val secondaryPeakStart: LocalTime
        get() = wakeResult.time.plusMinutes(480) // T+8.0h

    val secondaryPeakEnd: LocalTime
        get() = wakeResult.time.plusMinutes(630) // T+10.5h

    fun peakWindowFormatted(): String =
        String.format("%02d:%02d - %02d:%02d", peakStart.hour, peakStart.minute, peakEnd.hour, peakEnd.minute)

    fun secondaryPeakWindowFormatted(): String =
        String.format("%02d:%02d - %02d:%02d", secondaryPeakStart.hour, secondaryPeakStart.minute, secondaryPeakEnd.hour, secondaryPeakEnd.minute)
}

/**
 * Computes the 24-hour Adaptive Circadian Curve using Bayesian shrinkage and
 * Monotone Cubic Hermite Spline (Spec 2026-11-03 §3.3 & §3.4).
 */
class ComputeCircadianCurveUseCase @Inject constructor(
    private val detectWakeTime: DetectWakeTimeUseCase,
    private val focusRepository: FocusRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(
        nowMillis: Long = timeProvider.nowMillis(),
        zoneId: ZoneId = timeProvider.zoneId(),
    ): CircadianCurveData {
        val wakeResult = detectWakeTime(nowMillis, zoneId)
        val wakeTime = wakeResult.time
        val wakeMinuteOfDay = wakeTime.hour * 60 + wakeTime.minute

        // Fetch rated focus sessions in past 30 days
        val since = nowMillis - THIRTY_DAYS_MS
        val ratedSessions = focusRepository.getRatedSessionsSince(since)
        val allRatings = ratedSessions.mapNotNull { it.rating }

        // Group normalized ratings into 12 two-hour buckets
        val bucketRatings = List(12) { mutableListOf<Float>() }
        for (session in ratedSessions) {
            val r = session.rating ?: continue
            val rNorm = RateFocusSessionUseCase.normalize(r, allRatings)
            val sessionTime = Instant.ofEpochMilli(session.startedAt).atZone(zoneId).toLocalTime()
            val sMin = sessionTime.hour * 60 + sessionTime.minute
            val deltaMin = (sMin - wakeMinuteOfDay + 1440) % 1440
            val bucketIdx = (deltaMin / 120).coerceIn(0, 11)
            bucketRatings[bucketIdx].add(rNorm)
        }

        // Bayesian shrinkage calculation for 12 buckets
        val bucketCapacities = FloatArray(12) { b ->
            val n = bucketRatings[b].size
            val meanR = if (n > 0) bucketRatings[b].sum() / n else 0f
            val prior = BIOLOGICAL_PRIORS[b]
            ((n * meanR + K_SHRINKAGE * prior) / (n + K_SHRINKAGE)).coerceIn(0f, 1f)
        }.toList()

        // Build continuous Fritsch-Carlson Hermite Spline knots over [0h..24h]
        val segments = buildSplineSegments(bucketCapacities)

        // Evaluate NOW
        val nowTime = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalTime()
        val nowMin = nowTime.hour * 60 + nowTime.minute
        val nowHoursFromWake = ((nowMin - wakeMinuteOfDay + 1440) % 1440) / 60f

        val tempCurve = CircadianCurveData(
            wakeResult = wakeResult,
            bucketCapacities = bucketCapacities,
            segments = segments,
            currentCapacity = 0f,
            currentZoneLabel = "",
        )
        val currentCapacity = tempCurve.evaluate(nowHoursFromWake)
        val zoneLabel = resolveZoneLabel(nowHoursFromWake, currentCapacity)

        return tempCurve.copy(
            currentCapacity = currentCapacity,
            currentZoneLabel = zoneLabel,
        )
    }

    private fun buildSplineSegments(bucketCapacities: List<Float>): List<SplineSegment> {
        val y0 = bucketCapacities[0]
        val y11 = bucketCapacities[11]
        val boundaryY = (y0 + y11) / 2f

        // 14 Knots: 0h, 1h, 3h, 5h, ..., 23h, 24h
        val knotsX = FloatArray(14)
        val knotsY = FloatArray(14)

        knotsX[0] = 0f
        knotsY[0] = boundaryY

        for (b in 0..11) {
            knotsX[b + 1] = (b * 2 + 1).toFloat()
            knotsY[b + 1] = bucketCapacities[b]
        }

        knotsX[13] = 24f
        knotsY[13] = boundaryY

        val numIntervals = 13
        val delta = FloatArray(numIntervals)
        val h = FloatArray(numIntervals)
        for (i in 0 until numIntervals) {
            h[i] = knotsX[i + 1] - knotsX[i]
            delta[i] = if (h[i] > 0f) (knotsY[i + 1] - knotsY[i]) / h[i] else 0f
        }

        // Tangents
        val d = FloatArray(14)
        d[0] = delta[0]
        d[13] = delta[numIntervals - 1]
        for (i in 1 until 13) {
            d[i] = (delta[i - 1] + delta[i]) / 2f
        }

        // Fritsch-Carlson monotonicity adjustment
        for (i in 0 until numIntervals) {
            val del = delta[i]
            if (del == 0f) {
                d[i] = 0f
                d[i + 1] = 0f
            } else {
                val alpha = d[i] / del
                val beta = d[i + 1] / del
                if (alpha < 0f) d[i] = 0f
                if (beta < 0f) d[i + 1] = 0f
                val dist = alpha * alpha + beta * beta
                if (dist > 9f) {
                    val tau = 3f / sqrt(dist)
                    d[i] = tau * alpha * del
                    d[i + 1] = tau * beta * del
                }
            }
        }

        // Convert Hermite segments to cubic Bezier segments
        val segments = mutableListOf<SplineSegment>()
        for (i in 0 until numIntervals) {
            val hi = h[i]
            val x0 = knotsX[i]
            val y_0 = knotsY[i]
            val x1 = knotsX[i + 1]
            val y_1 = knotsY[i + 1]

            val cp1X = x0 + hi / 3f
            val cp1Y = (y_0 + d[i] * hi / 3f).coerceIn(0f, 1f)
            val cp2X = x1 - hi / 3f
            val cp2Y = (y_1 - d[i + 1] * hi / 3f).coerceIn(0f, 1f)

            segments.add(
                SplineSegment(
                    startX = x0,
                    startY = y_0.coerceIn(0f, 1f),
                    cp1X = cp1X,
                    cp1Y = cp1Y,
                    cp2X = cp2X,
                    cp2Y = cp2Y,
                    endX = x1,
                    endY = y_1.coerceIn(0f, 1f),
                )
            )
        }
        return segments
    }

    companion object {
        const val K_SHRINKAGE = 5f
        const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000L

        /** 12-phase biological priors (Spec 2026-11-03 §3.3). */
        val BIOLOGICAL_PRIORS = floatArrayOf(
            0.35f, // 0: T+0..2h Inersia Bangun
            0.95f, // 1: T+2..4h Peak Window #1
            0.85f, // 2: T+4..6h Produktivitas Tinggi
            0.40f, // 3: T+6..8h Circadian Dip
            0.75f, // 4: T+8..10h Peak Window #2
            0.60f, // 5: T+10..12h Fokus Menengah
            0.45f, // 6: T+12..14h Penurunan Energi
            0.25f, // 7: T+14..16h Fase Melatonin
            0.15f, // 8: T+16..18h Fase Tidur Awal
            0.10f, // 9: T+18..20h Fase Tidur Dalam
            0.15f, // 10: T+20..22h Tidur REM
            0.25f, // 11: T+22..24h Persiapan Bangun
        )

        fun resolveZoneLabel(hoursFromWake: Float, capacity: Float): String {
            val h = (hoursFromWake % 24f + 24f) % 24f
            return when {
                h in 2f..4.5f -> "Puncak Utama"
                h in 6f..8f -> "Lembah Energi"
                h in 8f..10.5f -> "Puncak Kedua"
                capacity >= 0.75f -> "Fokus Tinggi"
                capacity in 0.50f..0.74f -> "Fokus Sedang"
                capacity in 0.30f..0.49f -> "Transisi / Santai"
                else -> "Fase Istirahat"
            }
        }
    }
}
