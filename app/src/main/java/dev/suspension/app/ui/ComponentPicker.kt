package dev.suspension.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.suspension.app.R
import dev.suspension.app.data.CUSTOM_ID
import dev.suspension.app.data.ComponentCatalog
import dev.suspension.app.data.ForkModel
import dev.suspension.app.data.ReboundMode
import dev.suspension.app.data.ShockModel
import dev.suspension.app.ui.components.GroupCard
import dev.suspension.app.ui.components.RowDivider
import dev.suspension.app.ui.components.StepperRow
import dev.suspension.app.ui.theme.AppTheme

@Composable
internal fun PickerScaffold(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = type.screenTitle, color = colors.ink)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 8.dp),
            ) {
                Text(text = stringResource(R.string.picker_close), style = type.rowLabel, color = colors.dim)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { content() }
        }
    }
}

@Composable
internal fun CatalogRow(name: String, details: List<String>, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = name, onClick = onClick)
            .background(if (selected) colors.hit else colors.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = name, style = type.rowLabel, color = colors.ink)
        details.forEach { Text(text = it, style = type.rowHint, color = colors.dim) }
    }
}

@Composable
internal fun CustomTextField(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(text = label, style = type.rowLabel, color = colors.ink)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.hit)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            if (value.isEmpty()) {
                Text(text = placeholder, style = type.body, color = colors.dim)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = colors.ink, fontSize = type.body.fontSize, fontFamily = type.body.fontFamily),
                cursorBrush = SolidColor(colors.ink),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BoolToggleRow(label: String, value: Boolean, onToggle: () -> Unit) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClickLabel = label, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = type.rowLabel, color = colors.ink, modifier = Modifier.weight(1f))
        Text(text = stringResource(if (value) R.string.picker_yes else R.string.picker_no), style = type.rowLabel, color = colors.dim)
    }
}

@Composable
internal fun ApplyButton(label: String = stringResource(R.string.picker_custom_apply), onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.hit)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Text(text = label, style = AppTheme.type.rowLabel, color = colors.ink)
    }
}

/** "Model year 2025" / "Model years 2025, 2026"; null when no source document names one. */
@Composable
private fun modelYearsText(years: List<Int>): String? =
    if (years.isEmpty()) null else pluralStringResource(R.plurals.picker_model_years, years.size, years.sorted().joinToString(", "))

@Composable
private fun clickSummary(lscMax: Int, hscMax: Int?, reboundMode: ReboundMode, reboundMax: Int, hsrMax: Int?): String {
    val hsc = hscMax?.toString() ?: stringResource(R.string.picker_none)
    val rebound = if (reboundMode == ReboundMode.SPLIT && hsrMax != null) {
        stringResource(R.string.picker_split_rebound, reboundMax, hsrMax)
    } else {
        reboundMax.toString()
    }
    return stringResource(R.string.picker_clicks_summary, lscMax, hsc, rebound)
}

@Composable
fun ForkPickerOverlay(
    currentForkId: String,
    initialCustomFork: ForkModel,
    onSelectCatalog: (ForkModel) -> Unit,
    onSaveCustom: (ForkModel) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type

    PickerScaffold(title = stringResource(R.string.picker_title_fork), onDismiss = onDismiss) {
        GroupCard {
            ComponentCatalog.forks.forEachIndexed { index, fork ->
                if (index > 0) RowDivider()
                val details = buildList {
                    add(clickSummary(fork.lscMax, fork.hscMax, fork.reboundMode, fork.reboundMax, fork.hsrMax))
                    if (fork.pressureChart != null) add(stringResource(R.string.picker_has_chart))
                    modelYearsText(fork.modelYears)?.let(::add)
                }
                CatalogRow(
                    name = "${fork.displayName} — ${stringResource(R.string.picker_travel, fork.travelMm)}",
                    details = details,
                    selected = fork.id == currentForkId,
                    onClick = { onSelectCatalog(fork); onDismiss() },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 20.dp)) {
            Text(text = stringResource(R.string.picker_custom_title), style = type.groupHeading, color = colors.ink)
            Text(text = stringResource(R.string.picker_custom_no_weight_scaling), style = type.rowHint, color = colors.dim)

            var name by remember { mutableStateOf(initialCustomFork.displayName) }
            var travel by remember { mutableStateOf(initialCustomFork.travelMm.toDouble()) }
            var lscMax by remember { mutableStateOf(initialCustomFork.lscMax.toDouble()) }
            var hscAvailable by remember { mutableStateOf(initialCustomFork.hscMax != null) }
            var hscMax by remember { mutableStateOf((initialCustomFork.hscMax ?: 8).toDouble()) }
            var split by remember { mutableStateOf(initialCustomFork.reboundMode == ReboundMode.SPLIT) }
            var reboundMax by remember { mutableStateOf(initialCustomFork.reboundMax.toDouble()) }
            var hsrMax by remember { mutableStateOf((initialCustomFork.hsrMax ?: 8).toDouble()) }
            var psi by remember { mutableStateOf(initialCustomFork.baselinePsi) }

            GroupCard {
                CustomTextField(
                    label = stringResource(R.string.picker_custom_name_label),
                    value = name,
                    placeholder = stringResource(R.string.picker_custom_name_placeholder),
                    onValueChange = { name = it },
                )
                RowDivider()
                StepperRow(colors.spring, stringResource(R.string.picker_custom_travel_fork), stringResource(R.string.unit_mm), null, travel.toInt().toString(), { travel = (travel - 5).coerceAtLeast(80.0) }, { travel = (travel + 5).coerceAtMost(220.0) })
                RowDivider()
                StepperRow(colors.comp, stringResource(R.string.picker_custom_lsc_max), null, null, lscMax.toInt().toString(), { lscMax = (lscMax - 1).coerceAtLeast(1.0) }, { lscMax = (lscMax + 1).coerceAtMost(40.0) })
                RowDivider()
                BoolToggleRow(stringResource(R.string.picker_custom_hsc_available), hscAvailable) { hscAvailable = !hscAvailable }
                if (hscAvailable) {
                    RowDivider()
                    StepperRow(colors.comp, stringResource(R.string.picker_custom_hsc_max), null, null, hscMax.toInt().toString(), { hscMax = (hscMax - 1).coerceAtLeast(1.0) }, { hscMax = (hscMax + 1).coerceAtMost(40.0) })
                }
                RowDivider()
                BoolToggleRow(stringResource(R.string.picker_custom_rebound_split), split) { split = !split }
                RowDivider()
                StepperRow(colors.reb, stringResource(R.string.picker_custom_rebound_max), null, null, reboundMax.toInt().toString(), { reboundMax = (reboundMax - 1).coerceAtLeast(1.0) }, { reboundMax = (reboundMax + 1).coerceAtMost(40.0) })
                if (split) {
                    RowDivider()
                    StepperRow(colors.reb, stringResource(R.string.picker_custom_hsr_max), null, null, hsrMax.toInt().toString(), { hsrMax = (hsrMax - 1).coerceAtLeast(1.0) }, { hsrMax = (hsrMax + 1).coerceAtMost(40.0) })
                }
                RowDivider()
                StepperRow(colors.spring, stringResource(R.string.picker_custom_psi_baseline), stringResource(R.string.unit_psi), null, psi.toInt().toString(), { psi = (psi - 5).coerceAtLeast(20.0) }, { psi = (psi + 5).coerceAtMost(300.0) })
            }

            ApplyButton {
                onSaveCustom(
                    ForkModel(
                        id = CUSTOM_ID,
                        displayName = name.trim(),
                        travelMm = travel.toInt(),
                        lscMax = lscMax.toInt(),
                        hscMax = if (hscAvailable) hscMax.toInt() else null,
                        reboundMode = if (split) ReboundMode.SPLIT else ReboundMode.SINGLE,
                        reboundMax = reboundMax.toInt(),
                        hsrMax = if (split) hsrMax.toInt() else null,
                        pressureChart = null,
                        baselinePsi = psi,
                        maxPressurePsi = null,
                        spacersStock = null,
                        spacersMax = null,
                    ),
                )
                onDismiss()
            }
        }
    }
}

@Composable
fun ShockPickerOverlay(
    currentShockId: String,
    initialCustomShock: ShockModel,
    onSelectCatalog: (ShockModel) -> Unit,
    onSaveCustom: (ShockModel) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type

    PickerScaffold(title = stringResource(R.string.picker_title_shock), onDismiss = onDismiss) {
        GroupCard {
            ComponentCatalog.shocks.forEachIndexed { index, shock ->
                if (index > 0) RowDivider()
                val details = buildList {
                    add(clickSummary(shock.lscMax, shock.hscMax, shock.reboundMode, shock.reboundMax, shock.hsrMax))
                    if (shock.hasClimbLever) add(stringResource(R.string.picker_has_lever))
                    modelYearsText(shock.modelYears)?.let(::add)
                }
                CatalogRow(
                    name = "${shock.displayName} — ${stringResource(R.string.picker_stroke, shock.eyeToEyeMm, shock.strokeMm)}",
                    details = details,
                    selected = shock.id == currentShockId,
                    onClick = { onSelectCatalog(shock); onDismiss() },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 20.dp)) {
            Text(text = stringResource(R.string.picker_custom_title), style = type.groupHeading, color = colors.ink)

            var name by remember { mutableStateOf(initialCustomShock.displayName) }
            var stroke by remember { mutableStateOf(initialCustomShock.strokeMm.toDouble()) }
            var eyeToEye by remember { mutableStateOf(initialCustomShock.eyeToEyeMm.toDouble()) }
            var lscMax by remember { mutableStateOf(initialCustomShock.lscMax.toDouble()) }
            var hscAvailable by remember { mutableStateOf(initialCustomShock.hscMax != null) }
            var hscMax by remember { mutableStateOf((initialCustomShock.hscMax ?: 8).toDouble()) }
            var split by remember { mutableStateOf(initialCustomShock.reboundMode == ReboundMode.SPLIT) }
            var reboundMax by remember { mutableStateOf(initialCustomShock.reboundMax.toDouble()) }
            var hsrMax by remember { mutableStateOf((initialCustomShock.hsrMax ?: 8).toDouble()) }
            var lever by remember { mutableStateOf(initialCustomShock.hasClimbLever) }
            var rate by remember { mutableStateOf(initialCustomShock.customSpringLbs ?: 500.0) }

            GroupCard {
                CustomTextField(
                    label = stringResource(R.string.picker_custom_name_label),
                    value = name,
                    placeholder = stringResource(R.string.picker_custom_name_placeholder),
                    onValueChange = { name = it },
                )
                RowDivider()
                StepperRow(colors.spring, stringResource(R.string.picker_custom_stroke_shock), stringResource(R.string.unit_mm), null, stroke.toInt().toString(), { stroke = (stroke - 5).coerceAtLeast(30.0) }, { stroke = (stroke + 5).coerceAtMost(90.0) })
                RowDivider()
                StepperRow(colors.spring, stringResource(R.string.picker_custom_eye_to_eye_shock), stringResource(R.string.unit_mm), null, eyeToEye.toInt().toString(), { eyeToEye = (eyeToEye - 5).coerceAtLeast(150.0) }, { eyeToEye = (eyeToEye + 5).coerceAtMost(270.0) })
                RowDivider()
                StepperRow(colors.comp, stringResource(R.string.picker_custom_lsc_max), null, null, lscMax.toInt().toString(), { lscMax = (lscMax - 1).coerceAtLeast(1.0) }, { lscMax = (lscMax + 1).coerceAtMost(40.0) })
                RowDivider()
                BoolToggleRow(stringResource(R.string.picker_custom_hsc_available), hscAvailable) { hscAvailable = !hscAvailable }
                if (hscAvailable) {
                    RowDivider()
                    StepperRow(colors.comp, stringResource(R.string.picker_custom_hsc_max), null, null, hscMax.toInt().toString(), { hscMax = (hscMax - 1).coerceAtLeast(1.0) }, { hscMax = (hscMax + 1).coerceAtMost(40.0) })
                }
                RowDivider()
                BoolToggleRow(stringResource(R.string.picker_custom_rebound_split), split) { split = !split }
                RowDivider()
                StepperRow(colors.reb, stringResource(R.string.picker_custom_rebound_max), null, null, reboundMax.toInt().toString(), { reboundMax = (reboundMax - 1).coerceAtLeast(1.0) }, { reboundMax = (reboundMax + 1).coerceAtMost(40.0) })
                if (split) {
                    RowDivider()
                    StepperRow(colors.reb, stringResource(R.string.picker_custom_hsr_max), null, null, hsrMax.toInt().toString(), { hsrMax = (hsrMax - 1).coerceAtLeast(1.0) }, { hsrMax = (hsrMax + 1).coerceAtMost(40.0) })
                }
                RowDivider()
                BoolToggleRow(stringResource(R.string.picker_custom_lever), lever) { lever = !lever }
                RowDivider()
                StepperRow(colors.spring, stringResource(R.string.picker_custom_rate_baseline), stringResource(R.string.unit_lbs), null, rate.toInt().toString(), { rate = (rate - 25).coerceAtLeast(150.0) }, { rate = (rate + 25).coerceAtMost(900.0) })
            }

            ApplyButton {
                onSaveCustom(
                    ShockModel(
                        id = CUSTOM_ID,
                        displayName = name.trim(),
                        strokeMm = stroke.toInt(),
                        eyeToEyeMm = eyeToEye.toInt(),
                        lscMax = lscMax.toInt(),
                        hscMax = if (hscAvailable) hscMax.toInt() else null,
                        reboundMode = if (split) ReboundMode.SPLIT else ReboundMode.SINGLE,
                        reboundMax = reboundMax.toInt(),
                        hsrMax = if (split) hsrMax.toInt() else null,
                        hasClimbLever = lever,
                        customSpringLbs = rate,
                        preloadHintResId = R.string.hint_s_pre_generic,
                        preloadRangeResId = R.string.range_preload_generic,
                    ),
                )
                onDismiss()
            }
        }
    }
}
