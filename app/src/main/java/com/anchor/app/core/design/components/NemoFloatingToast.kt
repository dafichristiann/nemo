package com.anchor.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.GeistFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Floating toast notification in NEMO style:
 * Pill shape, ink background, onInk text (Manrope font), accent fog action button.
 */
@Composable
fun NemoFloatingToast(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onDismiss: () -> Unit = {},
    durationMillis: Long? = null,
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnActionClick by rememberUpdatedState(onActionClick)

    LaunchedEffect(message, actionLabel) {
        val displayDurationMs = durationMillis ?: if (actionLabel != null) 4000L else 2500L
        delay(displayDurationMs)
        currentOnDismiss()
    }

    Row(
        modifier = modifier
            .clip(PillShape)
            .background(Nemo.colors.ink)
            .padding(horizontal = Spacing.x4, vertical = Spacing.x3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = GeistFamily),
            color = Nemo.colors.onInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.width(Spacing.x3))
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = GeistFamily),
                color = Nemo.colors.fog,
                modifier = Modifier
                    .pressable(
                        shape = PillShape,
                        onClick = { currentOnActionClick?.invoke() },
                    )
                    .padding(vertical = 2.dp, horizontal = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NemoFloatingToastPreview() {
    NemoTheme {
        Box(
            modifier = Modifier
                .background(Nemo.colors.background)
                .padding(Spacing.x4),
        ) {
            NemoFloatingToast(
                message = "Task completed",
                actionLabel = "Undo",
                onActionClick = {},
            )
        }
    }
}
