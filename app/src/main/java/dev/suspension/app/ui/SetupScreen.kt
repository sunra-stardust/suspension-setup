package dev.suspension.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.suspension.app.R
import dev.suspension.app.data.BikeProfile
import dev.suspension.app.data.ComponentKind
import dev.suspension.app.data.ForkModel
import dev.suspension.app.data.Group
import dev.suspension.app.data.RotationDirection
import dev.suspension.app.data.RotationLogic
import dev.suspension.app.data.RowSpec
import dev.suspension.app.data.Scenario
import dev.suspension.app.data.ScenarioData
import dev.suspension.app.data.ShockModel
import dev.suspension.app.data.Stripe
import dev.suspension.app.data.TEMP_STEP_C
import dev.suspension.app.data.ValueRepository
import dev.suspension.app.ui.components.DampingRow
import dev.suspension.app.ui.components.GroupCard
import dev.suspension.app.ui.components.RowDivider
import dev.suspension.app.ui.components.ScenarioTabs
import dev.suspension.app.ui.components.StepperRow
import dev.suspension.app.ui.components.ToggleRow
import dev.suspension.app.ui.format.formatPercent
import dev.suspension.app.ui.format.formatStepValue
import dev.suspension.app.ui.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SetupScreen(
    repository: ValueRepository,
    selectedScenario: Scenario,
    onScenarioSelected: (Scenario) -> Unit,
    listState: LazyListState,
    bike: BikeProfile,
    fork: ForkModel,
    shock: ShockModel,
    weightKg: Double,
    onWeightChange: (Double) -> Unit,
    tempC: Int,
    onTempChange: (Int) -> Unit,
    onOpenForkPicker: () -> Unit,
    onOpenShockPicker: () -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type

    val groups = remember(fork, shock, weightKg, tempC, bike) {
        listOfNotNull(
            ScenarioData.buildForkGroup(fork, weightKg, tempC),
            ScenarioData.buildShockGroup(shock, weightKg, tempC, bike),
            ScenarioData.buildTiresGroup(bike, tempC),
            ScenarioData.buildFrameGroup(bike),
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky header — belongs to Setup only (spec §5).
        Column(modifier = Modifier.background(colors.surface)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_header_title, stringResource(bike.nameResId)),
                    style = type.screenTitle,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                // Read-only reminder of the conditions everything below is calculated for.
                Text(
                    text = stringResource(R.string.header_conditions, weightKg.roundToInt(), tempC),
                    style = type.body,
                    color = colors.dim,
                )
            }
            ScenarioTabs(
                scenarios = Scenario.ordered,
                selected = selectedScenario,
                labelFor = { stringResource(it.labelResId) },
                onSelect = onScenarioSelected,
            )
            RowDivider()
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                ConditionsCard(weightKg, onWeightChange, tempC, onTempChange)
            }
            items(groups) { group ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GroupHeadingText(
                        group = group,
                        onClick = when (group.component) {
                            ComponentKind.FORK -> onOpenForkPicker
                            ComponentKind.SHOCK -> onOpenShockPicker
                            null -> null
                        },
                    )
                    GroupCard {
                        group.rows.forEachIndexed { index, row ->
                            if (index > 0) RowDivider()
                            ParamRow(repository, row, selectedScenario, group.component)
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.setup_footer_note),
                    style = type.rowHint,
                    color = colors.dim,
                )
            }
        }
    }
}

/**
 * Rider weight and riding temperature: plain quantities (`+`/`−`, Change 01 §2) that apply to
 * every scenario. Both are global — the weather doesn't change with the terrain category.
 */
@Composable
private fun ConditionsCard(
    weightKg: Double,
    onWeightChange: (Double) -> Unit,
    tempC: Int,
    onTempChange: (Int) -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.group_conditions), style = type.groupHeading, color = colors.ink)
        GroupCard {
            StepperRow(
                stripe = colors.neutral,
                label = stringResource(R.string.label_rider_weight),
                unit = stringResource(R.string.unit_kg),
                hint = stringResource(R.string.hint_rider_weight),
                valueText = weightKg.roundToInt().toString(),
                onDecrement = { onWeightChange(weightKg - 1) },
                onIncrement = { onWeightChange(weightKg + 1) },
            )
            RowDivider()
            StepperRow(
                stripe = colors.neutral,
                label = stringResource(R.string.label_temperature),
                unit = stringResource(R.string.unit_celsius),
                hint = stringResource(R.string.hint_temperature),
                valueText = tempC.toString(),
                onDecrement = { onTempChange(tempC - TEMP_STEP_C) },
                onIncrement = { onTempChange(tempC + TEMP_STEP_C) },
            )
        }
    }
}

@Composable
private fun GroupHeadingText(group: Group, onClick: (() -> Unit)?) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val changeHint = stringResource(R.string.change_component_hint)
    Column(
        modifier = if (onClick != null) Modifier.clickable(onClickLabel = changeHint, onClick = onClick) else Modifier,
    ) {
        Text(text = group.heading.resolve(), style = type.groupHeading, color = colors.ink)
        if (onClick != null) {
            Text(text = changeHint, style = type.rowHint, color = colors.dim)
        }
    }
}

/** Rotation control (Change 01): every damping circuit — LSC/HSC/LSR/HSR or a single rebound. */
private fun isDampingRow(row: RowSpec): Boolean = row.stripe == Stripe.COMP || row.stripe == Stripe.REB

@Composable
private fun ParamRow(repository: ValueRepository, row: RowSpec, scenario: Scenario, component: ComponentKind?) {
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors
    val label = stringResource(row.labelResId)
    val hint = row.hint?.resolve()
    val stripeColor = row.stripe.color(colors)

    when (row) {
        is RowSpec.Stepper -> {
            val default = row.defaults[scenario.index]
            val flow = remember(row, scenario) { repository.stepperValue(row, scenario) }
            val value by flow.collectAsState(initial = default)

            if (isDampingRow(row)) {
                val componentPrefix = when (component) {
                    ComponentKind.FORK -> stringResource(R.string.prefix_gabel)
                    ComponentKind.SHOCK -> stringResource(R.string.prefix_daempfer)
                    null -> ""
                }
                val max = row.max ?: 0.0
                DampingRow(
                    stripe = stripeColor,
                    label = label,
                    hint = hint,
                    valueText = formatStepValue(value, row.step),
                    isOpen = value > 0.0,
                    isRebound = row.stripe == Stripe.REB,
                    atZero = value <= 0.0,
                    atMax = value >= max,
                    a11yBase = "$componentPrefix $label".trim(),
                    onClockwise = {
                        scope.launch {
                            repository.setStepperValue(row, scenario, RotationLogic.nextValue(RotationDirection.CLOCKWISE, value, max))
                        }
                    },
                    onCounterClockwise = {
                        scope.launch {
                            repository.setStepperValue(row, scenario, RotationLogic.nextValue(RotationDirection.COUNTER_CLOCKWISE, value, max))
                        }
                    },
                )
                return
            }

            val unit = row.unitResId?.let { stringResource(it) }
            val displayHint = if (row.derivedPercentDivisor != null) {
                stringResource(R.string.sag_percent_hint, formatPercent(value, row.derivedPercentDivisor))
            } else {
                hint
            }
            StepperRow(
                stripe = stripeColor,
                label = label,
                unit = unit,
                hint = displayHint,
                valueText = formatStepValue(value, row.step),
                onDecrement = { scope.launch { repository.setStepperValue(row, scenario, value - row.step) } },
                onIncrement = { scope.launch { repository.setStepperValue(row, scenario, value + row.step) } },
            )
        }
        is RowSpec.Toggle -> {
            val default = row.defaults[scenario.index]
            val flow = remember(row, scenario) { repository.toggleValue(row, scenario) }
            val value by flow.collectAsState(initial = default)
            ToggleRow(
                stripe = stripeColor,
                label = label,
                hint = hint,
                valueText = value,
                onToggle = {
                    val currentIndex = row.options.indexOf(value).coerceAtLeast(0)
                    val next = row.options[(currentIndex + 1) % row.options.size]
                    scope.launch { repository.setToggleValue(row, scenario, next) }
                },
            )
        }
    }
}
