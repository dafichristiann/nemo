package com.anchor.app.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.anchor.app.R

/**
 * Inter is bundled in `res/font` (weights 400/500/600/700).
 * Docs/superpowers/design/06-design-system.md §3: all text uses Inter.
 */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),   // 400
    Font(R.font.inter_medium, FontWeight.Medium),    // 500
    Font(R.font.inter_semibold, FontWeight.SemiBold), // 600
    Font(R.font.inter_bold, FontWeight.Bold),        // 700
)

/**
 * Font scale (docs 06-design-system.md §3.1 — actual `.pen` sizes 8..68).
 *
 * The scale is continuous in the mockup; these named styles capture the recurring
 * sizes/weights grouped by role. Sizes 8–10 occur only in the hero mini-phone mockup
 * and are intentionally omitted from the app type scale.
 */
object AnchorType {

    // --- Display / decorative ---
    val hero = TextStyle(fontSize = 68.sp, fontWeight = FontWeight.Bold, lineHeight = 72.sp)
    val heroSlogan = TextStyle(fontSize = 50.sp, fontWeight = FontWeight.Bold, lineHeight = 56.sp)
    val display36 = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp)

    // --- Display ---
    val display32 = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp)
    val display28 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
    val display26 = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)

    // --- Title ---
    val title24 = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
    val title22 = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
    val title20 = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)

    // --- Heading ---
    val heading18 = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
    val heading16 = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
    val heading16Semi = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)

    // --- Body ---
    val body15Semi = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
    val body14Semi = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
    val body14Medium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
    val body14 = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)

    // --- Label ---
    val label13Semi = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
    val label13Medium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
    val label13 = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp)

    // --- Caption ---
    val caption12Bold = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
    val caption12Semi = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp)
    val caption12Medium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
    val caption12 = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp)

    // --- Micro ---
    val micro11Bold = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
    val micro11Semi = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp)
    val micro11Medium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp)
    val micro11 = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, lineHeight = 14.sp)
}

/**
 * Material3 [Typography] mapped onto the Anchor scale so default Material components
 * (and any future ones) stay on-brand.
 */
val AnchorTypography = Typography(
    displayLarge = AnchorType.display36,
    displayMedium = AnchorType.display32,
    displaySmall = AnchorType.display28,
    headlineLarge = AnchorType.title24,
    headlineMedium = AnchorType.title22,
    headlineSmall = AnchorType.title20,
    titleLarge = AnchorType.heading18,
    titleMedium = AnchorType.heading16,
    titleSmall = AnchorType.heading16Semi,
    bodyLarge = AnchorType.body14,
    bodyMedium = AnchorType.label13,
    bodySmall = AnchorType.caption12,
    labelLarge = AnchorType.body14Semi,
    labelMedium = AnchorType.label13Medium,
    labelSmall = AnchorType.micro11Medium,
)
