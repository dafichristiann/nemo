package com.anchor.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Semua token warna Nemo. Netral murni: hitam, putih, abu. Tidak ada warna aksen. */
@Immutable
data class NemoColors(
    val background: Color,
    val surface: Color,      // sheet, dialog, kartu halus
    val fog: Color,          // input, chip tidak aktif, track
    val line: Color,         // garis pemisah, border
    val mute: Color,         // teks sekunder (kontras AA)
    val placeholder: Color,
    val inkSecondary: Color,
    val ink: Color,          // teks utama + isian tombol utama
    val onInk: Color,        // teks di atas ink
    val danger: Color,       // lembut, bukan merah menyala
    val dangerLine: Color,
    val scrim: Color,
    val scrimAlpha: Float,
)

val NemoDay = NemoColors(
    background = Color(0xFFFFFFFF), surface = Color(0xFFFAFAFA), fog = Color(0xFFF5F5F5),
    line = Color(0xFFE9E9E9), mute = Color(0xFF767676), placeholder = Color(0xFF8C8C8C),
    inkSecondary = Color(0xFF444444), ink = Color(0xFF111111), onInk = Color(0xFFFFFFFF),
    danger = Color(0xFF8A3B3B), dangerLine = Color(0xFFEAD9D9),
    scrim = Color(0xFF000000), scrimAlpha = 0.42f,
)

val NemoNight = NemoColors(
    background = Color(0xFF0A0A0A), surface = Color(0xFF141414), fog = Color(0xFF1B1B1B),
    line = Color(0xFF262626), mute = Color(0xFF8A8A8A), placeholder = Color(0xFF6B6B6B),
    inkSecondary = Color(0xFFCFCFCF), ink = Color(0xFFF2F2F2), onInk = Color(0xFF0A0A0A),
    danger = Color(0xFFD9A0A0), dangerLine = Color(0xFF3A2A2A),
    scrim = Color(0xFF000000), scrimAlpha = 0.67f,
)
