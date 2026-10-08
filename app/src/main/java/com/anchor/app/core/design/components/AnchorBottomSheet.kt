package com.anchor.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.AnchorTheme
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing

/**
 * Bottom sheet wrapper (docs/superpowers/design/06-design-system.md §7.9).
 *
 * White sheet with rounded top corners + a small centered drag handle. Used for the
 * Add Task sheet and future modal flows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
        containerColor = AnchorColors.surface,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        dragHandle = { SheetHandle() },
        content = content,
    )
}

/** The small 48x4 centered handle (`#E0E0E0`). */
@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.x3),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(Radius.track))
                .background(AnchorColors.border),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SheetHandlePreview() {
    AnchorTheme {
        Column { SheetHandle() }
    }
}
