package com.anchor.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.core.design.theme.ManropeFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

/** Fixed row height (dp) — one row is one selectable value. */
private val RowHeight = 40.dp

/** Number of rows visible at once; the middle row is the selection. */
private const val VisibleRows = 5

/**
 * A snapping scroll wheel for single-value selection (companion to 06 §7.7).
 *
 * Values scroll vertically; the row resting under the fixed center highlight is the selection.
 * There is deliberately **no keyboard / text field** — the only input is the scroll gesture,
 * matching the calm, deterministic feel of the rest of Anchor. When the scroll settles, the row
 * nearest the center is reported via [onSelect].
 *
 * @param values ordered options (top → bottom).
 * @param selectedIndex currently selected index into [values].
 * @param onSelect called with the settled index.
 * @param formatValue maps a value to its display string.
 */
@Composable
fun <T> WheelPicker(
    values: List<T>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    formatValue: (T) -> String = { it.toString() },
) {
    if (values.isEmpty()) return

    val listState = rememberLazyListState()
    val verticalPadding = RowHeight * (VisibleRows / 2)

    // Keep the wheel scrolled to the externally-selected index (first composition, or a reset
    // from the ViewModel) without fighting the user's own scrolling.
    LaunchedEffect(selectedIndex, values.size) {
        listState.scrollToItem(selectedIndex.coerceIn(0, values.lastIndex))
    }

    // When scrolling settles, find the row nearest the center and report it.
    LaunchedEffect(listState, values.size) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { inProgress ->
                if (!inProgress) {
                    val index = nearestCenterIndex(listState) ?: return@collect
                    if (index != selectedIndex) onSelect(index)
                }
            }
    }

    val centeredIndex by remember {
        derivedStateOf { nearestCenterIndex(listState) ?: selectedIndex }
    }

    Box(
        modifier = modifier
            .height(RowHeight * VisibleRows)
            .clip(RoundedCornerShape(Radius.card))
            .background(Nemo.colors.fog),
        contentAlignment = Alignment.Center,
    ) {
        // Center highlight band behind the rows.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(RowHeight)
                .padding(horizontal = Spacing.x4)
                .clip(RoundedCornerShape(Radius.field))
                .background(Nemo.colors.surface),
        )

        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = verticalPadding),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(values) { index, value ->
                val selected = index == centeredIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RowHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = formatValue(value),
                        fontFamily = ManropeFamily,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = if (selected) 20.sp else 15.sp,
                        textAlign = TextAlign.Center,
                        color = if (selected) Nemo.colors.ink else Nemo.colors.mute,
                    )
                }
            }
        }
    }
}

/** Index of the visible row whose vertical center is closest to the viewport center, or null. */
private fun nearestCenterIndex(state: androidx.compose.foundation.lazy.LazyListState): Int? {
    val info = state.layoutInfo
    val center = info.viewportStartOffset + (info.viewportEndOffset - info.viewportStartOffset) / 2
    return info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - center) }?.index
}

/** Renders whole seconds as `m:ss` (e.g. 90 → "1:30", 5 → "0:05"). */
fun formatDurationSeconds(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%d:%02d".format(safe / 60, safe % 60)
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun WheelPickerPreview() {
    NemoTheme {
        WheelPicker(
            values = (1..120).map { it * 5 },
            selectedIndex = 4,
            onSelect = {},
            formatValue = { formatDurationSeconds(it) },
            modifier = Modifier.padding(Spacing.x4),
        )
    }
}
