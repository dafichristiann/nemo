package com.anchor.app.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.anchor.app.R

/**
 * NEMO typography (docs/superpowers/newdesign/nemo-design-kit/nemo-kit/docs/design-spec.md §4).
 *
 * - **Fraunces** (Light 300 / Regular 400) for display and headings.
 * - **Manrope** (400/500/600/700) for everything else.
 *
 * Both are variable fonts bundled in `res/font` (SIL OFL). Variable fonts need minSdk 26,
 * which this app already targets. Weights are selected via [FontVariation.Settings].
 */
@OptIn(ExperimentalTextApi::class)
private fun variableFont(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val ManropeFamily = FontFamily(
    variableFont(R.font.manrope_variable, 400),
    variableFont(R.font.manrope_variable, 500),
    variableFont(R.font.manrope_variable, 600),
    variableFont(R.font.manrope_variable, 700),
)

val FrauncesFamily = FontFamily(
    variableFont(R.font.fraunces_variable, 300),
    variableFont(R.font.fraunces_variable, 400),
)

/** Material3 [Typography] mapped onto the NEMO scale (design-spec.md §4 table). */
val NemoTypography = Typography(
    displayLarge = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.Light, fontSize = 46.sp, lineHeight = 52.sp, letterSpacing = (-0.9).sp),
    displayMedium = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.Light, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.7).sp),
    headlineLarge = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.Light, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.Normal, fontSize = 19.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, lineHeight = 14.sp, letterSpacing = 1.3.sp),
)
