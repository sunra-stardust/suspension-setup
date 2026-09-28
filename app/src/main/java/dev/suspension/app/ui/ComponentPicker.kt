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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.suspension.app.R
import dev.suspension.app.data.ComponentCatalog
import dev.suspension.app.data.ForkModel
import dev.suspension.app.data.ReboundMode
import dev.suspension.app.data.ShockModel
import dev.suspension.app.ui.components.GroupCard
import dev.suspension.app.ui.components.RowDivider
import dev.suspension.app.ui.components.StepperRow
import dev.suspension.app.ui.theme.AppTheme

@Composable
private fun PickerScaffold(
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = type.screenTitle, color = colors.ink)
            Text(
                text = stringResource(R.string.picker_close),
                style = type.rowLabel,
                color = colors.dim,
                modifier = Modifier.clickable(onClick = onDismiss),
            )
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
private fun CatalogRow(name: String, detail: String, selected: Boolean, onClick: () -> Unit) {
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
        Text(text = detail, style = type.rowHint, color = colors.dim)
    }
}

@Composable
private fun CustomTextField(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(text = label, style = type.rowLabel, color = colors.ink)
        androidx.compose.foundation.layout.Box(
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
            .clickable(onClickLabel = label, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = type.rowLabel, color = colors.ink)
        Text(text = if (value) "Ja" else "Nein", style = type.rowLabel, color = colors.dim)
    }
}

@Composable
fun ForkPickerOverlay(
    currentForkId: String,
    initialCustomFork: ForkModel,
    initialCustomPsi: Double,
    onSelectCatalog: (ForkModel) -> Unit,
    onSaveCustom: (ForkModel, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type

    PickerScaffold(title = stringResource(R.string.picker_title_fork), onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GroupCard {
                ComponentCatalog.forks.forEachIndexed { index, fork ->
                    if (index > 0) RowDivider()
                    val hsc = fork.hscMax?.toString() ?: stringResource(R.string.picker_none)
                    val rebound = if (fork.reboundMode == ReboundMode.SPLIT) {
                        stringResource(R.string.picker_split_rebound, fork.reboundMax, fork.hsrMax ?: 0)
                    } else {
                        fork.reboundMax.toString()
                    }
                    CatalogRow(
                        name = "${fork.displayName} — ${stringResource(R.string.picker_travel, fork.travelMm)}",
                        detail = stringResource(R.string.picker_clicks_summary, fork.lscMax, hsc, rebound),
                        selected = fork.id == currentForkId,
                        onClick = { onSelectCatalog(fork); onDismiss() },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 20.dp)) {
            Text(text = stringResource(R.string.picker_custom_title), style = type.groupHeading, color = colors.ink)
            Text(text = stringResource(R.string.picker_custom_no_weight_scaling), style = type.rowHint, color = colors.dim)

            var name by remember { mutableStateOf(initialCustomFork.displayName.takeIf { it != "Eigene Gabel" } ?: "") }
            var travel by remember { mutableStateOf(initialCustomFork.travelMm.toDouble()) }
            var lscMax by remember { mutableStateOf(initialCustomFork.lscMax.toDouble()) }
            var hscAvailable by remember { mutableStateOf(initialCustomFork.hscMax != null) }
            var hscMax by remember { mutableStateOf((initialCustomFork.hscMax ?: 8).toDouble()) }
            var split by remember { mutableStateOf(initialCustomFork.reboundMode == ReboundMode.SPLIT) }
            var reboundMax by remember { mutableStateOf(initialCustomFork.reboundMax.toDouble()) }
            var hsrMax by remember { mutableStateOf((initialCustomFork.hsrMax ?: 8).toDouble()) }
            var psi by remember { mutableStateOf(initialCustomPsi) }

            GroupCard {
                CustomTextField(
                    label = stringResource(R.string.picker_custom_name_label),
                    value = name,
                    placeholder = stringResource(R.string.picker_custom_name_placeholder),
                    onValueChange = { name = it },
                )
                RowDivider()
                StepperRow(colors.spring, stringResource(R.string.picker_custom_travel_fork), "mm", null, travel.toInt().toString(), { travel = (travel - 5).coerceAtLeast(80.0) }, { travel = travel + 5 })
                RowDivider()
                StepperRow(colors.comp, stringResource(R.string.picker_custom_lsc_max), null, null, lscMax.toInt().toString(), { lscMax = (lscMax - 1).coerceAtLeast(1.0) }, { lscMax = lscMax + 1 })
                RowDivider()
                BoolToggleRow(stringResource(R.string.picker_custom_hsc_available), hscAvailable) { hscAvailable = !hscAvailable }
                if (hscAvailable) {
                    RowDivider()
                    StepperRow(colors.comp, stringResource(R.string.picker_custom_hsc_max), null, null, hscMax.toInt().toString(), { hscMax = (hscMax - 1).coerceAtLeast(1.0) }, { hscMax = hscMax + 1 })
                }
                RowDivider()
                BoolToggleRow(stringResource(R.string.picker_custom_rebound_split), split) { split = !split }
                RowDivider()
                StepperRow(colors.reb, stringResource(R.string.picker_custom_rebound_max), null, null, reboundMax.toInt().toString(), { reboundMax = (reboundMax - 1).coerceAtLeast(1.0) }, { reboundMax = reboundMax + 1 })
                if (split) {
                    RowDivider()
                    StepperRow(colors.reb, stringResource(R.string.picker_custom_hsr_max), null, null, hsrMax.toInt().toString(), { hsrMax = (hsrMax - 1).coerceAtLeast(1.0) }, { hsrMax = hsrMax + 1 })
                }
                RowDivider()
                StepperRow(colors.spring, stringResource(R.string.picker_custom_psi_baseline), "psi", null, psi.toInt().toString(), { psi = (psi - 5).coerceAtLeast(20.0) }, { psi = psi + 5 })
            }

            androidx.compose.foundation.layout.Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.hit)
                    .clickable {
                        val model = ForkModel(
                            id = dev.suspension.app.data.CUSTOM_ID,
                            displayName = name.ifBlank { "Eigene Gabel" },
                            travelMm = travel.toInt(),
                            lscMax = lscMax.toInt(),
                            hscMax = if (hscAvailable) hscMax.toInt() else null,
                            reboundMode = if (split) ReboundMode.SPLIT else ReboundMode.SINGLE,
                            reboundMax = reboundMax.toInt(),
                            hsrMax = if (split) hsrMax.toInt() else null,
                            pressureTable = emptyList(),
                        )
                        onSaveCustom(model, psi)
                        onDismiss()
                    }
                    .padding(vertical = 14.dp),
            ) {
                Text(text = stringResource(R.string.picker_custom_apply), style = type.rowLabel, color = colors.ink)
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GroupCard {
                ComponentCatalog.shocks.forEachIndexed { index, shock ->
                    if (index > 0) RowDivider()
                    val hsc = shock.hscMax?.toString() ?: stringResource(R.string.picker_none)
                    val rebound = if (shock.reboundMode == ReboundMode.SPLIT) {
                        stringResource(R.string.picker_split_rebound, shock.reboundMax, shock.hsrMax ?: 0)
                    } else {
                        shock.reboundMax.toString()
                    }
                    CatalogRow(
                        name = "${shock.displayName} — ${stringResource(R.string.picker_stroke, shock.eyeToEyeMm, shock.strokeMm)}",
                        detail = stringResource(R.string.picker_clicks_summary, shock.lscMax, hsc, rebound),
                        selected = shock.id == currentShockId,
                        onClick = { onSelectCatalog(shock); onDismiss() },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 20.dp)) {
            Text(text = stringResource(R.string.picker_custom_title), style = type.groupHeading, color = colors.ink)
            Text(text = stringResource(R.string.picker_custom_no_weight_scaling), style = type.rowHint, color = colors.dim)

            var name by remember { mutableStateOf(initialCustomShock.displayName.takeIf { it != "Eigener Dämpfer" } ?: "") }
            var stroke by remember { mutableStateOf(initialCustomShock.strokeMm.toDouble()) }
            var eyeToEye by remember { mutableStateOf(initialCustomShock.eyeToEyeMm.toDouble()) }
            var lscMax by remember { mutableStateOf(initialCustomShock.lscMax.toDouble()) }
            var hscAvailable by remember { mutableStateOf(initialCustomShock.hscMax != null) }
            var hscMax by remember { mutableStateOf((initialCustomShock.hscMax ?: 8).toDouble()) }
            var split by remember { mutableStateOf(initialCustomShock.reboundMode == ReboundMode.SPLIT) }
            var reboundMax by remember { mutableStateOf(initialCustomShock.reboundMax.toDouble()) }
            var hsrMax by remember { mutableStateOf((initialCustomShock.hsrMax ?: 8).toDouble()) }
            var rate by remember { mutableStateOf(initialCustomShock.referenceRateLbs) }

            GroupCard {
                CustomTextField(
                    label = stringResource(R.string.picker_custom_name_label),
                    value = name,
                    placeholder = stringResource(R.string.picker_custom_name_placeholder),
                    onValueChange = { name = it },
                )
                RowDivider()
                StepperRow(colors.spring, stringResource(R.string.picker_custom_stroke_shock), "mm", null, stroke.toInt().toString(), { stroke = (stroke - 5).coerceAtLeast(30.0) }, { stroke = stroke + 5 })
                RowDivider()
                StepperRow(colors.spring, stringResource(R.string.picker_custom_eye_to_eye_shock), "mm", null, eyeToEye.toInt().toString(), { eyeToEye = (eyeToEye - 5).coerceAtLeast(100.0) }, { eyeToEye = eyeToEye + 5 })
                RowDivider()
                StepperRow(colors.comp, stringResource(R.string.picker_custom_lsc_max), null, null, lscMax.toInt().toString(), { lscMax = (lscMax - 1).coerceAtLeast(1.0) }, { lscMax = lscMax + 1 })
                RowDivider()
                BoolToggleRow(stringResource(R.string.picker_custom_hsc_available), hscAvailable) { hscAvailable = !hscAvailable }
                if (hscAvailable) {
                    RowDivider()
                    StepperRow(colors.comp, stringResource(R.string.picker_custom_hsc_max), null, null, hscMax.toInt().toString(), { hscMax = (hscMax - 1).coerceAtLeast(1.0) }, { hscMax = hscMax + 1 })
                }
                RowDivider()
                BoolToggleRow(stringResource(R.string.picker_custom_rebound_split), split) { split = !split }
                RowDivider()
                StepperRow(colors.reb, stringResource(R.string.picker_custom_rebound_max), null, null, reboundMax.toInt().toString(), { reboundMax = (reboundMax - 1).coerceAtLeast(1.0) }, { reboundMax = reboundMax + 1 })
                if (split) {
                    RowDivider()
                    StepperRow(colors.reb, stringResource(R.string.picker_custom_hsr_max), null, null, hsrMax.toInt().toString(), { hsrMax = (hsrMax - 1).coerceAtLeast(1.0) }, { hsrMax = hsrMax + 1 })
                }
                RowDivider()
                StepperRow(colors.spring, stringResource(R.string.picker_custom_rate_baseline), "lbs", null, rate.toInt().toString(), { rate = (rate - 25).coerceAtLeast(100.0) }, { rate = rate + 25 })
            }

            androidx.compose.foundation.layout.Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.hit)
                    .clickable {
                        val model = ShockModel(
                            id = dev.suspension.app.data.CUSTOM_ID,
                            displayName = name.ifBlank { "Eigener Dämpfer" },
                            strokeMm = stroke.toInt(),
                            eyeToEyeMm = eyeToEye.toInt(),
                            lscMax = lscMax.toInt(),
                            hscMax = if (hscAvailable) hscMax.toInt() else null,
                            reboundMode = if (split) ReboundMode.SPLIT else ReboundMode.SINGLE,
                            reboundMax = reboundMax.toInt(),
                            hsrMax = if (split) hsrMax.toInt() else null,
                            referenceWeightKg = dev.suspension.app.data.DEFAULT_WEIGHT_KG,
                            referenceRateLbs = rate,
                            rateSlopeLbsPerKg = 0.0,
                            rateStepLbs = 25.0,
                        )
                        onSaveCustom(model)
                        onDismiss()
                    }
                    .padding(vertical = 14.dp),
            ) {
                Text(text = stringResource(R.string.picker_custom_apply), style = type.rowLabel, color = colors.ink)
            }
        }
    }
}
