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
import dev.suspension.app.data.ComponentKind
import dev.suspension.app.data.ForkModel
import dev.suspension.app.data.Group
import dev.suspension.app.data.GroupHeading
import dev.suspension.app.data.RowSpec
import dev.suspension.app.data.Scenario
import dev.suspension.app.data.ScenarioData
import dev.suspension.app.data.ShockModel
import dev.suspension.app.data.ValueRepository
import dev.suspension.app.ui.components.GroupCard
import dev.suspension.app.ui.components.RowDivider
import dev.suspension.app.ui.components.ScenarioTabs
import dev.suspension.app.ui.components.StepButton
import dev.suspension.app.ui.components.StepperRow
import dev.suspension.app.ui.components.ToggleRow
import dev.suspension.app.ui.format.formatPercent
import dev.suspension.app.ui.format.formatStepValue
import dev.suspension.app.ui.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    repository: ValueRepository,
    selectedScenario: Scenario,
    onScenarioSelected: (Scenario) -> Unit,
    listState: LazyListState,
    fork: ForkModel,
    shock: ShockModel,
    weightKg: Double,
    onWeightChange: (Double) -> Unit,
    onOpenForkPicker: () -> Unit,
    onOpenShockPicker: () -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type

    val groups = remember(fork, shock, weightKg) {
        listOf(
            ScenarioData.buildForkGroup(fork, weightKg),
            ScenarioData.buildShockGroup(shock, weightKg),
            ScenarioData.tiresGroup,
            ScenarioData.frameGroup,
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky header — belongs to Setup only (spec §5).
        Column(modifier = Modifier.background(colors.surface)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.app_header_title), style = type.screenTitle, color = colors.ink)
                WeightStepper(weightKg = weightKg, onChange = onWeightChange)
            }
            ScenarioTabs(
                scenarios = Scenario.ordered,
                selected = selectedScenario,
                labelFor = { scenarioLabel(it) },
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
                    if (group.component != null) {
                        Text(
                            text = stringResource(R.string.component_direction_legend),
                            style = type.rowHint,
                            color = colors.dim,
                        )
                    }
                    GroupCard {
                        group.rows.forEachIndexed { index, row ->
                            if (index > 0) RowDivider()
                            ParamRow(repository, row, selectedScenario)
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

@Composable
private fun WeightStepper(weightKg: Double, onChange: (Double) -> Unit) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val decreaseLabel = stringResource(R.string.weight_decrease)
    val increaseLabel = stringResource(R.string.weight_increase)
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(symbol = "−", contentDescription = decreaseLabel, onClick = { onChange((weightKg - 1).coerceAtLeast(30.0)) })
        Text(
            text = stringResource(R.string.weight_display, weightKg.toInt()),
            style = type.body,
            color = colors.dim,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        StepButton(symbol = "+", contentDescription = increaseLabel, onClick = { onChange((weightKg + 1).coerceAtMost(180.0)) })
    }
}

@Composable
private fun GroupHeadingText(group: Group, onClick: (() -> Unit)?) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val text = when (val h = group.heading) {
        is GroupHeading.Static -> stringResource(h.resId)
        is GroupHeading.Dynamic -> stringResource(h.templateResId, *h.args.toTypedArray())
    }
    Column {
        Text(
            text = text,
            style = type.groupHeading,
            color = colors.ink,
            modifier = if (onClick != null) Modifier.clickable(onClickLabel = stringResource(R.string.change_component_hint), onClick = onClick) else Modifier,
        )
        if (onClick != null) {
            Text(text = stringResource(R.string.change_component_hint), style = type.rowHint, color = colors.dim)
        }
    }
}

@Composable
private fun scenarioLabel(scenario: Scenario): String = stringResource(scenario.labelResId)

@Composable
private fun ParamRow(repository: ValueRepository, row: RowSpec, scenario: Scenario) {
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors
    val label = stringResource(row.labelResId)
    val hint = row.hintResId?.let { stringResource(it) }
    val stripeColor = row.stripe.color(colors)

    when (row) {
        is RowSpec.Stepper -> {
            val default = row.defaults[scenario.index]
            val flow = remember(row, scenario) { repository.stepperValue(row, scenario) }
            val value by flow.collectAsState(initial = default)
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
