package com.anchor.app.platform.audio

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.anchor.app.R

/**
 * 7 distinct soundscape lofi genres curated for deep focus and flow sessions.
 */
enum class LofiGenre(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    @RawRes val soundResId: Int,
    val emoji: String,
    val tag: String,
) {
    CHILL(
        id = "chill",
        titleRes = R.string.focus_lofi_chill_title,
        descRes = R.string.focus_lofi_chill_desc,
        soundResId = R.raw.nemo_lofi_chill,
        emoji = "☕",
        tag = "CHILL",
    ),
    JAZZ(
        id = "jazz",
        titleRes = R.string.focus_lofi_jazz_title,
        descRes = R.string.focus_lofi_jazz_desc,
        soundResId = R.raw.nemo_lofi_jazz,
        emoji = "🎷",
        tag = "JAZZ",
    ),
    CHRISTIAN(
        id = "christian",
        titleRes = R.string.focus_lofi_christian_title,
        descRes = R.string.focus_lofi_christian_desc,
        soundResId = R.raw.nemo_lofi_christian,
        emoji = "🕊️",
        tag = "WORSHIP",
    ),
    STUDY(
        id = "study",
        titleRes = R.string.focus_lofi_study_title,
        descRes = R.string.focus_lofi_study_desc,
        soundResId = R.raw.nemo_lofi_study,
        emoji = "💻",
        tag = "STUDY",
    ),
    RAIN(
        id = "rain",
        titleRes = R.string.focus_lofi_rain_title,
        descRes = R.string.focus_lofi_rain_desc,
        soundResId = R.raw.nemo_lofi_rain,
        emoji = "🌧️",
        tag = "COZY",
    ),
    SYNTHWAVE(
        id = "synthwave",
        titleRes = R.string.focus_lofi_synthwave_title,
        descRes = R.string.focus_lofi_synthwave_desc,
        soundResId = R.raw.nemo_lofi_synthwave,
        emoji = "🌆",
        tag = "RETRO",
    ),
    ACOUSTIC(
        id = "acoustic",
        titleRes = R.string.focus_lofi_acoustic_title,
        descRes = R.string.focus_lofi_acoustic_desc,
        soundResId = R.raw.nemo_lofi_acoustic,
        emoji = "🎹",
        tag = "PIANO",
    );

    companion object {
        val DEFAULT = CHILL
        val ALL: List<LofiGenre> = entries
        fun fromId(id: String?): LofiGenre = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
