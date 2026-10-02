package dev.suspension.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.suspension.app.R
import dev.suspension.app.data.BikeTraits
import dev.suspension.app.data.Trait
import dev.suspension.app.data.BikeProfile
import dev.suspension.app.data.ComponentKind
import dev.suspension.app.data.Deviation
import dev.suspension.app.data.Edit
import dev.suspension.app.data.ForkModel
import dev.suspension.app.data.Group
import dev.suspension.app.data.RotationDirection
import dev.suspension.app.data.RotationLogic
import dev.suspension.app.data.RowSpec
import dev.suspension.app.data.RowValues
import dev.suspension.app.data.ScenarioData
import dev.suspension.app.data.ShockModel
import dev.suspension.app.data.Stripe
import dev.suspension.app.data.TEMP_STEP_C
import dev.suspension.app.data.Vorlage
import dev.suspension.app.ui.components.ConfirmDialog
import dev.suspension.app.ui.components.DampingRow
import dev.suspension.app.ui.components.GroupCard
import dev.suspension.app.ui.components.NameDialog
import dev.suspension.app.ui.components.RowDivider
import dev.suspension.app.ui.components.ScenarioTabs
import dev.suspension.app.ui.components.StepperRow
import dev.suspension.app.ui.components.ToggleRow
import dev.suspension.app.ui.format.currentLocale
import dev.suspension.app.ui.format.formatPercent
import dev.suspension.app.ui.format.formatStepValue
import dev.suspension.app.ui.theme.AppTheme
import kotlin.math.roundToInt

@Composable
fun SetupScreen(
    vorlagen: List<Vorlage>,
    selectedVorlage: Vorlage,
    onVorlageSelected: (Vorlage) -> Unit,
    /** The rider's edits for this bike in [selectedVorlage], by row id. */
    edits: Map<String, Edit>,
    onEdit: (rowId: String, edit: Edit?) -> Unit,
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
    bikeName: String,
    /** What the app knows about this bike's parts (motor, Fox, air shock). */
    traits: BikeTraits,
    onOpenBikes: () -> Unit,
    onAddVorlage: (name: String) -> Unit,
    onRenameVorlage: (Vorlage, name: String) -> Unit,
    onDeleteVorlage: (Vorlage) -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    var vorlageDialog by rememberSaveable { mutableStateOf(VorlageDialog.NONE) }
    val switchBike = stringResource(R.string.bike_switch_hint)

    val groups = remember(fork, shock, weightKg, tempC, bike) {
        listOfNotNull(
            ScenarioData.buildForkGroup(fork, weightKg, tempC),
            ScenarioData.buildShockGroup(shock, weightKg, tempC, bike, ebike = traits.has(Trait.EBIKE)),
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
                // Tapping the title opens the bike list (switch, add, copy …).
                Text(
                    text = stringResource(R.string.app_header_title, bikeName) + " ▾",
                    style = type.screenTitle,
                    color = colors.ink,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clickable(onClickLabel = switchBike, onClick = onOpenBikes)
                        .wrapContentHeight(Alignment.CenterVertically),
                )
                // Read-only reminder of the conditions everything below is calculated for.
                Text(
                    text = stringResource(R.string.header_conditions, weightKg.roundToInt(), tempC),
                    style = type.body,
                    color = colors.dim,
                )
            }
            ScenarioTabs(
                vorlagen = vorlagen,
                selected = selectedVorlage,
                labelFor = { it.label() },
                onSelect = onVorlageSelected,
                addLabel = stringResource(R.string.vorlage_new),
                onAdd = { vorlageDialog = VorlageDialog.NEW },
            )
            // Own Vorlagen can be renamed and deleted; built-ins only adapted through their values.
            if (selectedVorlage.builtIn == null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp),
                ) {
                    TextAction(stringResource(R.string.action_rename)) { vorlageDialog = VorlageDialog.RENAME }
                    TextAction(stringResource(R.string.action_delete)) { vorlageDialog = VorlageDialog.DELETE }
                }
            }
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
                    val openPicker = when (group.component) {
                        ComponentKind.FORK -> onOpenForkPicker
                        ComponentKind.SHOCK -> onOpenShockPicker
                        null -> null
                    }
                    GroupHeadingText(group = group, onClick = openPicker)
                    val needsCheck = when (group.component) {
                        ComponentKind.FORK -> fork.needsCheck
                        ComponentKind.SHOCK -> shock.needsCheck
                        null -> false
                    }
                    if (needsCheck && openPicker != null) {
                        Text(
                            text = stringResource(R.string.setup_needs_check),
                            style = type.rowHint,
                            color = colors.ink,
                            modifier = Modifier.clickable(onClick = openPicker),
                        )
                    }
                    GroupCard {
                        group.rows.forEachIndexed { index, row ->
                            if (index > 0) RowDivider()
                            ParamRow(row, edits[row.id], { onEdit(row.id, it) }, group.component)
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(
                        // The ridefox.com tip only helps when there is a Fox part on the bike.
                        if (traits.foxOnBike) R.string.setup_footer_note
                        else R.string.setup_footer_note_generic,
                    ),
                    style = type.rowHint,
                    color = colors.dim,
                )
            }
        }
    }

    VorlageDialogs(
        dialog = vorlageDialog,
        selected = selectedVorlage,
        onAdd = onAddVorlage,
        onRename = onRenameVorlage,
        onDelete = onDeleteVorlage,
        onClose = { vorlageDialog = VorlageDialog.NONE },
    )
}

private enum class VorlageDialog { NONE, NEW, RENAME, DELETE }

@Composable
private fun VorlageDialogs(
    dialog: VorlageDialog,
    selected: Vorlage,
    onAdd: (String) -> Unit,
    onRename: (Vorlage, String) -> Unit,
    onDelete: (Vorlage) -> Unit,
    onClose: () -> Unit,
) {
    val selectedName = selected.label()
    when (dialog) {
        VorlageDialog.NEW -> NameDialog(
            title = stringResource(R.string.vorlage_new),
            hint = stringResource(R.string.vorlage_new_hint, selectedName),
            initial = "",
            onConfirm = { onAdd(it); onClose() },
            onDismiss = onClose,
        )
        VorlageDialog.RENAME -> NameDialog(
            title = stringResource(R.string.action_rename),
            hint = null,
            initial = selectedName,
            onConfirm = { onRename(selected, it); onClose() },
            onDismiss = onClose,
        )
        VorlageDialog.DELETE -> ConfirmDialog(
            text = stringResource(R.string.vorlage_delete_confirm, selectedName),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { onDelete(selected); onClose() },
            onDismiss = onClose,
        )
        VorlageDialog.NONE -> Unit
    }
}

@Composable
private fun TextAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = AppTheme.type.rowHint,
        color = AppTheme.colors.ink,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clickable(onClickLabel = label, onClick = onClick)
            .wrapContentHeight(Alignment.CenterVertically),
    )
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
private fun ParamRow(row: RowSpec, edit: Edit?, onEdit: (Edit?) -> Unit, component: ComponentKind?) {
    val colors = AppTheme.colors
    val label = stringResource(row.labelResId)
    val hint = row.hint?.resolve()
    val stripeColor = row.stripe.color(colors)
    val locale = currentLocale()

    when (row) {
        is RowSpec.Stepper -> {
            val value = RowValues.stepper(row, edit)
            val setValue = { newValue: Double -> onEdit(RowValues.editFor(row, newValue)) }
            val deviation = Deviation.of(row, value)?.text()

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
                    valueText = formatStepValue(value, row.step, locale),
                    isOpen = value > 0.0,
                    isRebound = row.stripe == Stripe.REB,
                    atZero = value <= 0.0,
                    atMax = value >= max,
                    a11yBase = "$componentPrefix $label".trim(),
                    onClockwise = { setValue(RotationLogic.nextValue(RotationDirection.CLOCKWISE, value, max)) },
                    onCounterClockwise = { setValue(RotationLogic.nextValue(RotationDirection.COUNTER_CLOCKWISE, value, max)) },
                    deviation = deviation,
                )
                return
            }

            val unit = row.unitResId?.let { stringResource(it) }
            val displayHint = if (row.derivedPercentDivisor != null) {
                stringResource(R.string.sag_percent_hint, formatPercent(value, row.derivedPercentDivisor, locale))
            } else {
                hint
            }
            StepperRow(
                stripe = stripeColor,
                label = label,
                unit = unit,
                hint = displayHint,
                valueText = formatStepValue(value, row.step, locale),
                onDecrement = { setValue(value - row.step) },
                onIncrement = { setValue(value + row.step) },
                deviation = deviation,
            )
        }
        is RowSpec.Toggle -> {
            val value = RowValues.toggle(row, edit)
            val deviation = Deviation.of(row, value)?.text { option -> row.optionLabels[option]?.let { stringResource(it) } ?: option }
            ToggleRow(
                stripe = stripeColor,
                label = label,
                hint = hint,
                valueText = row.optionLabels[value]?.let { stringResource(it) } ?: value,
                onToggle = {
                    val currentIndex = row.options.indexOf(value).coerceAtLeast(0)
                    onEdit(RowValues.editFor(row, row.options[(currentIndex + 1) % row.options.size]))
                },
                deviation = deviation,
            )
        }
    }
}

/** Built-in Vorlagen are translated; the rider's own carry the name they gave them. */
@Composable
fun Vorlage.label(): String = builtIn?.let { stringResource(it.labelResId) } ?: name.orEmpty()