package com.anchor.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.AnchorElevation
import com.anchor.app.core.design.theme.AnchorTheme
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.LocalAnchorElevation
import com.anchor.app.core.design.theme.Radius

/**
 * Primary CTA button with the mockup's soft-3D "Base/Front" pattern
 * (docs/superpowers/design/06-design-system.md §7.1).
 *
 * Front: `#0A0A0A`, radius 16, height 56. The darker Base sits behind, offset +6 down,
 * giving a lifted look without images. Label 16/600, white.
 *
 * Named "PrimaryButton" to match the reusable-component list in 07-screens.md §4.
 */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val elevation: AnchorElevation = LocalAnchorElevation.current
    Box(modifier = modifier.fillMaxWidth()) {
        // Soft-3D "Base" layer behind the front button.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = 6.dp)
                .height(56.dp)
                .shadow(
                    elevation = elevation.button.dp,
                    shape = Radius.buttonShape,
                    ambientColor = Color.Black.copy(alpha = 0.25f),
                    spotColor = Color.Black.copy(alpha = 0.25f),
                )
                .background(color = AnchorColors.depthBaseDark, shape = Radius.buttonShape),
        )
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(Radius.button),
            colors = ButtonDefaults.buttonColors(
                containerColor = AnchorColors.ink,
                contentColor = AnchorColors.surface,
                disabledContainerColor = AnchorColors.ink.copy(alpha = 0.35f),
                disabledContentColor = AnchorColors.surface.copy(alpha = 0.7f),
            ),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                text = label,
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonPreview() {
    AnchorTheme {
        PrimaryButton(label = "Get started", onClick = {})
    }
}
