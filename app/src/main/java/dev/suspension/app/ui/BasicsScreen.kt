package dev.suspension.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.suspension.app.R
import dev.suspension.app.data.ValueRepository
import dev.suspension.app.ui.components.GroupCard
import dev.suspension.app.ui.components.RowDivider
import dev.suspension.app.ui.components.TwoColumnRow
import dev.suspension.app.ui.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun BasicsScreen(
    repository: ValueRepository,
    listState: LazyListState,
    onResetDone: () -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val scope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            BasicsCard(stringResource(R.string.basics_clicks_title)) {
                Text(stringResource(R.string.basics_clicks_body_1), style = type.body, color = colors.ink)
                Text(stringResource(R.string.basics_clicks_body_2), style = type.body, color = colors.ink)
            }
        }
        item {
            BasicsCard(stringResource(R.string.basics_order_title)) {
                val steps = listOf(
                    R.string.basics_order_step_1, R.string.basics_order_step_2, R.string.basics_order_step_3,
                    R.string.basics_order_step_4, R.string.basics_order_step_5, R.string.basics_order_step_6,
                )
                steps.forEachIndexed { i, resId ->
                    Text("${i + 1}. ${stringResource(resId)}", style = type.body, color = colors.ink)
                }
                Text(stringResource(R.string.basics_order_footer), style = type.rowHint, color = colors.dim)
            }
        }
        item {
            BasicsCard(stringResource(R.string.basics_sag_title)) {
                Text(stringResource(R.string.basics_sag_fork), style = type.body, color = colors.ink)
                Text(stringResource(R.string.basics_sag_shock), style = type.body, color = colors.ink)
                Text(stringResource(R.string.basics_sag_footer), style = type.rowHint, color = colors.dim)
            }
        }
        item {
            BasicsCard(stringResource(R.string.basics_targets_title)) {
                GroupCard {
                    val rows = listOf(
                        R.string.basics_targets_row_1_label to R.string.basics_targets_row_1_value,
                        R.string.basics_targets_row_2_label to R.string.basics_targets_row_2_value,
                        R.string.basics_targets_row_3_label to R.string.basics_targets_row_3_value,
                        R.string.basics_targets_row_4_label to R.string.basics_targets_row_4_value,
                        R.string.basics_targets_row_5_label to R.string.basics_targets_row_5_value,
                        R.string.basics_targets_row_6_label to R.string.basics_targets_row_6_value,
                    )
                    rows.forEachIndexed { index, (labelRes, valueRes) ->
                        if (index > 0) RowDivider()
                        TwoColumnRow(stringResource(labelRes), stringResource(valueRes))
                    }
                }
                Text(stringResource(R.string.basics_targets_footer), style = type.rowHint, color = colors.dim)
            }
        }
        item {
            BasicsCard(stringResource(R.string.basics_temp_title)) {
                val bullets = listOf(
                    R.string.basics_temp_bullet_1, R.string.basics_temp_bullet_2,
                    R.string.basics_temp_bullet_3, R.string.basics_temp_bullet_4,
                )
                bullets.forEach { resId -> Text("• ${stringResource(resId)}", style = type.body, color = colors.ink) }
                Text(stringResource(R.string.basics_temp_footer), style = type.rowHint, color = colors.dim)
            }
        }
        item {
            BasicsCard(stringResource(R.string.basics_compression_title)) {
                Text(stringResource(R.string.basics_compression_body), style = type.body, color = colors.ink)
            }
        }
        item {
            BasicsCard(stringResource(R.string.basics_factory_title)) {
                GroupCard {
                    val rows = listOf(
                        R.string.basics_factory_row_1_label to R.string.basics_factory_row_1_value,
                        R.string.basics_factory_row_2_label to R.string.basics_factory_row_2_value,
                        R.string.basics_factory_row_3_label to R.string.basics_factory_row_3_value,
                        R.string.basics_factory_row_4_label to R.string.basics_factory_row_4_value,
                        R.string.basics_factory_row_5_label to R.string.basics_factory_row_5_value,
                        R.string.basics_factory_row_6_label to R.string.basics_factory_row_6_value,
                    )
                    rows.forEachIndexed { index, (labelRes, valueRes) ->
                        if (index > 0) RowDivider()
                        TwoColumnRow(stringResource(labelRes), stringResource(valueRes))
                    }
                }
                Text(stringResource(R.string.basics_factory_footer), style = type.rowHint, color = colors.dim)
            }
        }
        item {
            BasicsCard(stringResource(R.string.basics_ranges_title)) {
                GroupCard {
                    val rows = listOf(
                        R.string.basics_ranges_row_1_label to R.string.basics_ranges_row_1_value,
                        R.string.basics_ranges_row_2_label to R.string.basics_ranges_row_2_value,
                        R.string.basics_ranges_row_3_label to R.string.basics_ranges_row_3_value,
                        R.string.basics_ranges_row_4_label to R.string.basics_ranges_row_4_value,
                        R.string.basics_ranges_row_5_label to R.string.basics_ranges_row_5_value,
                        R.string.basics_ranges_row_6_label to R.string.basics_ranges_row_6_value,
                        R.string.basics_ranges_row_7_label to R.string.basics_ranges_row_7_value,
                        R.string.basics_ranges_row_8_label to R.string.basics_ranges_row_8_value,
                        R.string.basics_ranges_row_9_label to R.string.basics_ranges_row_9_value,
                        R.string.basics_ranges_row_10_label to R.string.basics_ranges_row_10_value,
                    )
                    rows.forEachIndexed { index, (labelRes, valueRes) ->
                        if (index > 0) RowDivider()
                        TwoColumnRow(stringResource(labelRes), stringResource(valueRes))
                    }
                }
                Text(stringResource(R.string.basics_ranges_footer), style = type.rowHint, color = colors.dim)
            }
        }
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.hit)
                    .clickable(onClickLabel = stringResource(R.string.reset_button)) {
                        scope.launch {
                            repository.resetAll()
                            onResetDone()
                        }
                    }
                    .padding(vertical = 14.dp),
            ) {
                Text(text = stringResource(R.string.reset_button), style = type.rowLabel, color = colors.ink)
            }
        }
        item {
            Text(
                text = stringResource(R.string.basics_persistence_note),
                style = type.rowHint,
                color = colors.dim,
            )
        }
    }
}

@Composable
private fun BasicsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = type.groupHeading, color = colors.ink)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}
