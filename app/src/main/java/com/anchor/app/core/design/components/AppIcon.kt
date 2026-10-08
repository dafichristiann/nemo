package com.anchor.app.core.design.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.InterFamily

/**
 * Renders an app's launcher icon (from a [Drawable]) as a small rounded square, with a
 * single-letter fallback when the icon is unavailable. Used by the app lists in Settings.
 */
@Composable
fun AppIcon(
    icon: Drawable?,
    fallbackLabel: String,
    modifier: Modifier = Modifier,
    size: Int = 40,
) {
    val shape = RoundedCornerShape(10.dp)
    if (icon != null) {
        val bitmap = icon.toBitmap(width = size, height = size)
        Box(
            modifier = modifier
                .size(size.dp)
                .clip(shape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(size.dp),
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(size.dp)
                .clip(shape)
                .background(AnchorColors.surfaceMuted),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = fallbackLabel.take(1).uppercase(),
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AnchorColors.textStrongAlt,
            )
        }
    }
}
