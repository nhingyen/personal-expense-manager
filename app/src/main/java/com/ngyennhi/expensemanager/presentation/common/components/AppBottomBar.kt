package com.ngyennhi.expensemanager.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.presentation.navigation.AppDestination
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

/** Material 3 surface with the measured Figma indicators and an independent Add action. */
@Composable
internal fun AppBottomBar(selected: AppDestination, onSelect: (AppDestination) -> Unit, onAdd: () -> Unit) {
    Surface(color = ExpenseManagerColors.surface, modifier = Modifier.testTag("app_bottom_bar")) {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))) {
            BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
                val measurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val labels = AppDestination.entries.map { stringResource(it.label) }
                val labelWidths = AppDestination.entries.mapIndexed { index, destination ->
                    val size = measurer.measure(labels[index], MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (destination == selected) FontWeight.SemiBold else FontWeight.Normal,
                    )).size
                    with(density) { size.width.toDp().value }
                }
                val availableWidth = (maxWidth.value - 16f).coerceAtLeast(1f)
                // Keep equal Figma slots normally. Share spare width with long localized
                // labels at larger text sizes before falling back to a taller, wrapped row.
                val minimumWidths = AppDestination.entries.mapIndexed { index, destination ->
                    maxOf(if (destination == AppDestination.ADD) 48f else 58f, labelWidths[index])
                }
                val adaptWidths = minimumWidths.sum() <= availableWidth &&
                    labelWidths.any { it > availableWidth / AppDestination.entries.size }
                val extraWidth = ((availableWidth - minimumWidths.sum()) / AppDestination.entries.size).coerceAtLeast(0f)
                Row(
                    Modifier.fillMaxWidth()
                        .selectableGroup().testTag("bottom_bar_content")
                        .height(IntrinsicSize.Min).heightIn(min = 80.dp).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppDestination.entries.forEachIndexed { index, destination ->
                        val isAdd = destination == AppDestination.ADD
                        val isSelected = destination == selected
                        val color = if (isSelected) ExpenseManagerColors.primary else ExpenseManagerColors.textSecondary
                        val action = if (isAdd) Modifier.clickable(role = Role.Button, onClick = onAdd)
                        else Modifier.selectable(isSelected, role = Role.Tab, onClick = { onSelect(destination) })
                        Column(
                            Modifier.weight(if (adaptWidths) minimumWidths[index] + extraWidth else 1f)
                                .fillMaxHeight().heightIn(min = 48.dp)
                                .testTag("nav_${destination.route}").then(action).padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                        ) {
                            Box(
                                Modifier.widthIn(max = if (isAdd) 48.dp else 58.dp).fillMaxWidth().height(32.dp)
                                    .testTag("nav_indicator_${destination.route}")
                                    .background(when {
                                        isAdd -> ExpenseManagerColors.primary
                                        isSelected -> ExpenseManagerColors.primaryContainer
                                        else -> ExpenseManagerColors.surface
                                    }, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(painterResource(destination.icon), null,
                                    Modifier.size(22.dp).testTag("nav_icon_${destination.route}"),
                                    tint = if (isAdd) ExpenseManagerColors.surface else color)
                            }
                            Text(labels[index], color = color,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center)
                        }
                    }
                }
                HorizontalDivider(color = ExpenseManagerColors.outline, thickness = 1.dp)
            }
        }
    }
}
