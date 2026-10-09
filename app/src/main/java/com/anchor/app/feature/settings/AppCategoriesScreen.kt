package com.anchor.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.AnchorSwitch
import com.anchor.app.core.design.components.AppIcon
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.AppCategory

/**
 * Sub-screen: manage app categories (FR-S3). Moves the long category list out of Settings.
 * Each row: app icon · package · editable category label + "treat as distraction" toggle.
 */
@Composable
fun AppCategoriesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val categories by viewModel.filteredCategories.collectAsStateWithLifecycle()
    val query by viewModel.categoryQuery.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorColors.surface)
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6),
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        BackHeader(title = stringResource(R.string.categories_title), onBack = onBack)

        SearchField(
            query = query,
            hint = stringResource(R.string.settings_categories_search_hint),
            onQueryChange = viewModel::setCategoryQuery,
        )

        if (categories.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_categories_empty),
                fontFamily = InterFamily,
                fontSize = 13.sp,
                color = AnchorColors.textFaint,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.x8),
                verticalArrangement = Arrangement.spacedBy(Spacing.x3),
            ) {
                items(categories, key = { it.packageName }) { category ->
                    CategoryManageRow(
                        category = category,
                        icon = viewModel.iconFor(category.packageName),
                        label = viewModel.labelFor(category.packageName),
                        onRename = { viewModel.onSetCategory(category.packageName, it) },
                        onToggleDistraction = { viewModel.onToggleDistraction(category) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryManageRow(
    category: AppCategory,
    icon: android.graphics.drawable.Drawable?,
    label: String,
    onRename: (String) -> Unit,
    onToggleDistraction: () -> Unit,
) {
    var name by remember(category.packageName, category.category) { mutableStateOf(category.category) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(icon = icon, fallbackLabel = label)
            Spacer(modifier = Modifier.width(Spacing.x3))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = AnchorColors.textPrimary,
                )
                Text(
                    text = category.packageName,
                    fontFamily = InterFamily,
                    fontSize = 11.sp,
                    color = AnchorColors.textFaint,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(Radius.field))
                    .background(AnchorColors.surfaceSunken)
                    .padding(horizontal = Spacing.x3),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (name.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_category_name_hint),
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        color = AnchorColors.textFaint,
                    )
                }
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = InterFamily, fontSize = 13.sp, color = AnchorColors.textPrimary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(Radius.chip))
                    .background(AnchorColors.ink)
                    .pressable(shape = RoundedCornerShape(Radius.chip), onClick = { onRename(name) })
                    .padding(horizontal = Spacing.x4),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.settings_category_save),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = AnchorColors.surface,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings_category_distraction),
                fontFamily = InterFamily,
                fontSize = 12.sp,
                color = AnchorColors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            AnchorSwitch(
                checked = category.isDistraction,
                onCheckedChange = { onToggleDistraction() },
            )
        }
    }
}
