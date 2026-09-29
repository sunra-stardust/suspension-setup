package dev.suspension.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import dev.suspension.app.data.BikeProfile
import dev.suspension.app.data.ForkModel
import dev.suspension.app.data.ReboundMode
import dev.suspension.app.data.RotationDirection
import dev.suspension.app.data.ShockModel
import dev.suspension.app.data.ValueRepository
import dev.suspension.app.data.roundToStep
import dev.suspension.app.ui.components.GroupCard
import dev.suspension.app.ui.components.RotationIcon
import dev.suspension.app.ui.components.RowDivider
import dev.suspension.app.ui.components.TwoColumnRow
import dev.suspension.app.ui.format.formatStepValue
import dev.suspension.app.ui.theme.AppTheme
import kotlinx.coroutines.launch

/** Target-sag rows: label, share of travel in %, fork (true) or shock (false). */
private val SAG_TARGETS = listOf(
    Triple(R.string.basics_targets_row_1_label, 15, true),
    Triple(R.string.basics_targets_row_2_label, 18, true),
    Triple(R.string.basics_targets_row_3_label, 20, true),
    Triple(R.string.basics_targets_row_4_label, 25, false),
    Triple(R.string.basics_targets_row_5_label, 30, false),
    Triple(R.string.basics_targets_row_6_label, 32, false),
)

@Composable
fun BasicsScreen(
    repository: ValueRepository,
    listState: LazyListState,
    bike: BikeProfile,
    fork: ForkModel,
    shock: ShockModel,
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
                GroupCard {
                    DirectionTableRow(
                        direction = RotationDirection.CLOCKWISE,
                        primary = stringResource(R.string.basics_clicks_row_cw_primary),
                        secondary1 = stringResource(R.string.basics_clicks_row_cw_secondary_1),
                        secondary2 = stringResource(R.string.basics_clicks_row_cw_secondary_2),
                    )
                    RowDivider()
                    DirectionTableRow(
                        direction = RotationDirection.COUNTER_CLOCKWISE,
                        primary = stringResource(R.string.basics_clicks_row_ccw_primary),
                        secondary1 = stringResource(R.string.basics_clicks_row_ccw_secondary_1),
                        secondary2 = stringResource(R.string.basics_clicks_row_ccw_secondary_2),
                    )
                }
                Text(stringResource(R.string.basics_clicks_body_1), style = type.body, color = colors.ink)
                Text(stringResource(R.string.basics_clicks_body_2), style = type.body, color = colors.ink)
                Text(stringResource(R.string.basics_clicks_body_3), style = type.body, color = colors.ink)
                Text(stringResource(R.string.basics_clicks_body_4), style = type.body, color = colors.ink)
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
                    SAG_TARGETS.forEachIndexed { index, (labelRes, percent, isFork) ->
                        if (index > 0) RowDivider()
                        val travel = if (isFork) fork.travelMm else shock.strokeMm
                        val step = if (isFork) 1.0 else 0.5
                        val mm = roundToStep(percent / 100.0 * travel, step)
                        TwoColumnRow(
                            stringResource(labelRes),
                            stringResource(R.string.basics_targets_value, formatStepValue(mm, step), percent),
                        )
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
        if (bike.factorySpec.isNotEmpty()) {
            item {
                BasicsCard(stringResource(R.string.basics_factory_title)) {
                    GroupCard {
                        bike.factorySpec.forEachIndexed { index, (labelRes, valueRes) ->
                            if (index > 0) RowDivider()
                            TwoColumnRow(stringResource(labelRes), stringResource(valueRes))
                        }
                    }
                    Text(stringResource(R.string.basics_factory_footer), style = type.rowHint, color = colors.dim)
                }
            }
        }
        item {
            BasicsCard(stringResource(R.string.basics_ranges_title)) {
                val gabel = stringResource(R.string.prefix_gabel)
                val daempfer = stringResource(R.string.prefix_daempfer)
                val lsc = stringResource(R.string.label_lsc)
                val hsc = stringResource(R.string.label_hsc)
                val lsr = stringResource(R.string.label_lsr)
                val hsr = stringResource(R.string.label_hsr)
                val rebound = stringResource(R.string.label_rebound)
                val preloadLabel = stringResource(R.string.basics_ranges_preload_label)
                val preloadRange = stringResource(shock.preloadRangeResId)
                val postLabel = stringResource(R.string.basics_ranges_post_label)
                val postRange = bike.dropper?.let { stringResource(it.rangeResId) }

                val rows = buildList {
                    add("$gabel $lsc" to fork.lscMax.toString())
                    fork.hscMax?.let { add("$gabel $hsc" to it.toString()) }
                    if (fork.reboundMode == ReboundMode.SPLIT) {
                        add("$gabel $lsr" to fork.reboundMax.toString())
                        fork.hsrMax?.let { add("$gabel $hsr" to it.toString()) }
                    } else {
                        add("$gabel $rebound" to fork.reboundMax.toString())
                    }
                    add("$daempfer $lsc" to shock.lscMax.toString())
                    shock.hscMax?.let { add("$daempfer $hsc" to it.toString()) }
                    if (shock.reboundMode == ReboundMode.SPLIT) {
                        add("$daempfer $lsr" to shock.reboundMax.toString())
                        shock.hsrMax?.let { add("$daempfer $hsr" to it.toString()) }
                    } else {
                        add("$daempfer $rebound" to shock.reboundMax.toString())
                    }
                    add(preloadLabel to preloadRange)
                    postRange?.let { add(postLabel to it) }
                }
                GroupCard {
                    rows.forEachIndexed { index, (label, value) ->
                        if (index > 0) RowDivider()
                        TwoColumnRow(label, value)
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

/** Icon-in-first-column table row for the Basics "Drehrichtung und Zählweise" card (Change 01 §7). */
@Composable
private fun DirectionTableRow(direction: RotationDirection, primary: String, secondary1: String, secondary2: String) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        RotationIcon(direction = direction, tint = colors.ink, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = primary, style = type.rowLabel, color = colors.ink)
            Text(text = secondary1, style = type.rowHint, color = colors.dim)
            Text(text = secondary2, style = type.rowHint, color = colors.dim)
        }
    }
}
